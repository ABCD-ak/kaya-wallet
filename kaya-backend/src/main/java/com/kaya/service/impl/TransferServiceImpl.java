package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.transaction.CounterpartyResponse;
import com.kaya.dto.transaction.ReceiptResponse;
import com.kaya.dto.transfer.CreateTransferRequest;
import com.kaya.dto.transfer.TransferQuoteRequest;
import com.kaya.dto.transfer.TransferQuoteResponse;
import com.kaya.dto.transfer.TransferRecipientInput;
import com.kaya.dto.transfer.TransferResponse;
import com.kaya.entity.Recipient;
import com.kaya.entity.Transaction;
import com.kaya.entity.Wallet;
import com.kaya.entity.WalletCard;
import com.kaya.mapper.RecipientMapper;
import com.kaya.mapper.TransactionMapper;
import com.kaya.mapper.WalletCardMapper;
import com.kaya.mapper.WalletMapper;
import com.kaya.service.NotificationService;
import com.kaya.service.TransactionService;
import com.kaya.service.TransferService;
import com.kaya.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class TransferServiceImpl implements TransferService {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.001");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999999.99");

    private final WalletMapper walletMapper;
    private final WalletCardMapper walletCardMapper;
    private final TransactionMapper transactionMapper;
    private final RecipientMapper recipientMapper;
    private final TransactionService transactionService;
    private final UserService userService;
    private final NotificationService notificationService;

    public TransferServiceImpl(
            WalletMapper walletMapper,
            WalletCardMapper walletCardMapper,
            TransactionMapper transactionMapper,
            RecipientMapper recipientMapper,
            TransactionService transactionService,
            UserService userService,
            NotificationService notificationService) {
        this.walletMapper = walletMapper;
        this.walletCardMapper = walletCardMapper;
        this.transactionMapper = transactionMapper;
        this.recipientMapper = recipientMapper;
        this.transactionService = transactionService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public TransferQuoteResponse quote(Long userId, TransferQuoteRequest request) {
        Long walletId = parseId(request.walletId(), "钱包 ID 不正确");
        Wallet wallet = requireWallet(userId, walletId);
        if (request.recipientId() != null) {
            requireRecipient(userId, parseId(request.recipientId(), "联系人 ID 不正确"));
        }

        BigDecimal amount = parseAmount(request.amount());
        BigDecimal fee = feeFor(amount);
        BigDecimal total = amount.add(fee);

        // 付款方式：选择银行卡时用该卡独立余额，未选卡时用零钱（钱包）余额
        BigDecimal available;
        String currencyCode;
        if (request.cardId() == null) {
            available = wallet.getBalance();
            currencyCode = wallet.getCurrencyCode();
        } else {
            WalletCard card = requireCard(userId, parseId(request.cardId(), "银行卡 ID 不正确"));
            available = card.getBalance();
            currencyCode = card.getCurrencyCode() == null ? wallet.getCurrencyCode() : card.getCurrencyCode();
        }

        return new TransferQuoteResponse(
                String.valueOf(wallet.getId()),
                currencyCode,
                money(amount),
                FEE_RATE.toPlainString(),
                money(fee),
                money(total),
                money(available),
                available.compareTo(total) >= 0);
    }

    @Override
    @Transactional
    public TransferResponse create(Long userId, String idempotencyKey, CreateTransferRequest request) {
        String key = requireIdempotencyKey(idempotencyKey);
        Transaction existing = transactionMapper.findByUserIdAndIdempotencyKey(userId, key);
        if (existing != null) {
            return buildTransfer(userId, existing);
        }

        Long walletId = parseId(request.walletId(), "钱包 ID 不正确");
        BigDecimal amount = parseAmount(request.amount());
        BigDecimal fee = feeFor(amount);
        BigDecimal total = amount.add(fee);

        Wallet wallet = walletMapper.findByIdAndUserId(walletId, userId);
        if (wallet == null) {
            throw new BusinessException(40401, "钱包不存在");
        }
        if (!"active".equals(wallet.getStatus())) {
            throw new BusinessException(40004, "钱包当前不可用");
        }

        // 支付密码校验：已设置的用户必须携带正确的支付密码，任何扣款前完成校验
        if (userService.hasPaymentPassword(userId)) {
            if (request.paymentPassword() == null || request.paymentPassword().isBlank()) {
                throw new BusinessException(40005, "请输入支付密码");
            }
            userService.verifyPaymentPassword(userId, request.paymentPassword());
        }

        // 付款方式：选择银行卡时只扣该卡独立余额；未选卡时从零钱（钱包）扣款
        WalletCard card = null;
        Wallet payWallet = wallet;
        if (request.cardId() != null) {
            card = requireCard(userId, parseId(request.cardId(), "银行卡 ID 不正确"));
            if (card.getBalance().compareTo(total) < 0) {
                throw new BusinessException(40003, "银行卡余额不足");
            }
        } else {
            payWallet = walletMapper.findByIdAndUserIdForUpdate(walletId, userId);
            if (payWallet == null) {
                throw new BusinessException(40401, "钱包不存在");
            }
            if (payWallet.getBalance().compareTo(total) < 0) {
                throw new BusinessException(40003, "零钱余额不足");
            }
        }

        Recipient recipient = resolveRecipient(userId, request);
        if (card != null) {
            if (walletCardMapper.deductBalance(card.getId(), userId, total) == 0) {
                throw new BusinessException(40906, "银行卡余额已变化，请重试");
            }
        } else {
            int updated = walletMapper.updateBalance(
                    walletId,
                    payWallet.getBalance().subtract(total),
                    payWallet.getVersion());
            if (updated == 0) {
                throw new BusinessException(40906, "零钱余额已变化，请重试");
            }
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Transaction transaction = new Transaction();
        transaction.setReferenceNo("TR-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 20).toUpperCase());
        transaction.setUserId(userId);
        transaction.setWalletId(walletId);
        transaction.setType("transfer_out");
        transaction.setDirection("debit");
        transaction.setStatus("success");
        transaction.setAmount(amount);
        transaction.setFeeAmount(fee);
        transaction.setCurrencyCode(
                card == null || card.getCurrencyCode() == null
                        ? wallet.getCurrencyCode()
                        : card.getCurrencyCode());
        transaction.setRecipientId(recipient.getId());
        transaction.setCounterpartyName(recipient.getDisplayName());
        transaction.setCounterpartyAccountNo(recipient.getAccountNo());
        transaction.setIdempotencyKey(key);
        transaction.setNote(trimToNull(request.note()));
        transaction.setOccurredAt(now);

        try {
            transactionMapper.insert(transaction);
        } catch (DuplicateKeyException ex) {
            Transaction duplicate = transactionMapper.findByUserIdAndIdempotencyKey(userId, key);
            if (duplicate != null) {
                return buildTransfer(userId, duplicate);
            }
            throw ex;
        }

        recipientMapper.touchLastUsed(recipient.getId(), now);

        // 账户变动通知：转账支出
        String symbol = currencySymbol(transaction.getCurrencyCode());
        notificationService.create(
                userId,
                "transfer_sent",
                "转账支出",
                "向 " + recipient.getDisplayName() + " 转账 " + symbol + money(amount),
                amount,
                transaction.getCurrencyCode(),
                transaction.getId());

        return buildTransfer(userId, transactionMapper.findByIdAndUserId(transaction.getId(), userId));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse get(Long userId, Long transferId) {
        Transaction transaction = transactionMapper.findByIdAndUserId(transferId, userId);
        if (transaction == null || !"transfer_out".equals(transaction.getType())) {
            throw new BusinessException(40401, "转账记录不存在");
        }
        return buildTransfer(userId, transaction);
    }

    private TransferResponse buildTransfer(Long userId, Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        BigDecimal fee = transaction.getFeeAmount() == null ? BigDecimal.ZERO : transaction.getFeeAmount();
        CounterpartyResponse recipient = transaction.getCounterpartyName() == null
                ? null
                : new CounterpartyResponse(
                        transaction.getCounterpartyUserId() == null
                                ? null
                                : String.valueOf(transaction.getCounterpartyUserId()),
                        transaction.getCounterpartyName(),
                        transaction.getCounterpartyAccountNo(),
                        null);
        ReceiptResponse receipt = transactionService.receipt(userId, transaction.getId());

        return new TransferResponse(
                String.valueOf(transaction.getId()),
                transaction.getReferenceNo(),
                String.valueOf(transaction.getWalletId()),
                transaction.getStatus(),
                money(amount),
                money(fee),
                money(amount.add(fee)),
                transaction.getCurrencyCode(),
                recipient,
                transaction.getNote(),
                transaction.getOccurredAt() == null
                        ? null
                        : transaction.getOccurredAt().atOffset(ZoneOffset.UTC),
                transaction.getRelatedTransactionId() == null
                        ? null
                        : String.valueOf(transaction.getRelatedTransactionId()),
                receipt);
    }

    private Recipient resolveRecipient(Long userId, CreateTransferRequest request) {
        if (request.recipientId() != null) {
            return requireRecipient(userId, parseId(request.recipientId(), "联系人 ID 不正确"));
        }
        TransferRecipientInput input = request.recipient();
        if (input == null) {
            throw new BusinessException(40001, "请选择收款人或填写收款信息");
        }

        String accountNo = input.accountNo().trim();
        Recipient existing = recipientMapper.findByUserIdAndAccountNo(userId, accountNo);
        if (existing != null) {
            return existing;
        }

        Recipient recipient = new Recipient();
        recipient.setUserId(userId);
        recipient.setDisplayName(input.displayName().trim());
        recipient.setAccountNo(accountNo);
        recipient.setBankName(trimToNull(input.bankName()));
        recipient.setFavorite(false);
        try {
            recipientMapper.insert(recipient);
            return recipient;
        } catch (DuplicateKeyException ex) {
            Recipient raced = recipientMapper.findByUserIdAndAccountNo(userId, accountNo);
            if (raced != null) {
                return raced;
            }
            throw ex;
        }
    }

    private Wallet requireWallet(Long userId, Long walletId) {
        Wallet wallet = walletMapper.findByIdAndUserId(walletId, userId);
        if (wallet == null) {
            throw new BusinessException(40401, "钱包不存在");
        }
        return wallet;
    }

    private WalletCard requireCard(Long userId, Long cardId) {
        WalletCard card = walletCardMapper.findByIdAndUserId(cardId, userId);
        if (card == null) {
            throw new BusinessException(40401, "银行卡不存在");
        }
        if (!"active".equals(card.getStatus())) {
            throw new BusinessException(40004, "银行卡当前不可用");
        }
        return card;
    }

    private Recipient requireRecipient(Long userId, Long recipientId) {
        Recipient recipient = recipientMapper.findByIdAndUserId(recipientId, userId);
        if (recipient == null) {
            throw new BusinessException(40401, "联系人不存在");
        }
        return recipient;
    }

    private BigDecimal feeFor(BigDecimal amount) {
        return amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }

    private String currencySymbol(String code) {
        if (code == null) {
            return "¥";
        }
        return switch (code) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "JPY" -> "¥";
            case "GBP" -> "£";
            case "HKD" -> "HK$";
            default -> "¥";
        };
    }

    private BigDecimal parseAmount(String value) {
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() <= 0 || amount.scale() > 2 || amount.compareTo(MAX_AMOUNT) > 0) {
                throw new BusinessException(40001, "金额需为两位小数的正数");
            }
            return amount.setScale(2);
        } catch (NumberFormatException ex) {
            throw new BusinessException(40001, "金额格式不正确");
        }
    }

    private Long parseId(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(40001, message);
        }
    }

    private String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(40001, "缺少 Idempotency-Key 请求头");
        }
        String key = value.trim();
        if (key.length() < 8 || key.length() > 64) {
            throw new BusinessException(40001, "Idempotency-Key 长度需为 8-64 位");
        }
        return key;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
