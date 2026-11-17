package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.transaction.ReceiptResponse;
import com.kaya.dto.transfer.CreateTransferRequest;
import com.kaya.dto.transfer.TransferQuoteRequest;
import com.kaya.dto.transfer.TransferQuoteResponse;
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
import com.kaya.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long WALLET_ID = 1L;
    private static final String IDEMPOTENCY_KEY = "idem-key-0001";

    @Mock private WalletMapper walletMapper;
    @Mock private WalletCardMapper walletCardMapper;
    @Mock private TransactionMapper transactionMapper;
    @Mock private RecipientMapper recipientMapper;
    @Mock private TransactionService transactionService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;

    private TransferServiceImpl transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(
                walletMapper, walletCardMapper, transactionMapper,
                recipientMapper, transactionService, userService, notificationService);
    }

    @Test
    @DisplayName("转账试算：手续费按 0.1% 四舍五入")
    void quoteCalculatesFee() {
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));

        TransferQuoteResponse quote = transferService.quote(USER_ID,
                new TransferQuoteRequest("1", null, null, null, "100.00"));

        assertEquals("100.00", quote.amount());
        assertEquals("0.10", quote.feeAmount());
        assertEquals("100.10", quote.totalAmount());
        assertEquals("1000.00", quote.availableBalance());
        assertTrue(quote.sufficientBalance());
    }

    @Test
    @DisplayName("转账试算：余额不足时 sufficientBalance=false")
    void quoteDetectsInsufficientBalance() {
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("50.00", 0));

        TransferQuoteResponse quote = transferService.quote(USER_ID,
                new TransferQuoteRequest("1", null, null, null, "100.00"));

        assertFalse(quote.sufficientBalance());
    }

    @Test
    @DisplayName("转账试算：选择银行卡时使用卡内独立余额")
    void quoteUsesCardBalance() {
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));
        when(walletCardMapper.findByIdAndUserId(3L, USER_ID)).thenReturn(card("200.00", "USD"));

        TransferQuoteResponse quote = transferService.quote(USER_ID,
                new TransferQuoteRequest("1", "3", null, null, "100.00"));

        assertEquals("200.00", quote.availableBalance());
        assertEquals("USD", quote.currencyCode());
        assertTrue(quote.sufficientBalance());
    }

    @Test
    @DisplayName("转账失败：缺少 Idempotency-Key 返回 40001")
    void createRequiresIdempotencyKey() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.create(USER_ID, " ", request(null, "9", "100.00", null)));

        assertEquals(40001, ex.getCode());
        verify(walletMapper, never()).updateBalance(anyLong(), any(), anyInt());
    }

    @Test
    @DisplayName("转账失败：金额超过两位小数返回 40001")
    void createRejectsInvalidAmount() {
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.create(USER_ID, IDEMPOTENCY_KEY,
                        request(null, "9", "1.234", null)));

        assertEquals(40001, ex.getCode());
    }

    @Test
    @DisplayName("转账失败：零钱余额不足返回 40003，且不扣款")
    void createRejectsInsufficientWalletBalance() {
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(null);
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("10.00", 0));
        when(userService.hasPaymentPassword(USER_ID)).thenReturn(false);
        when(walletMapper.findByIdAndUserIdForUpdate(WALLET_ID, USER_ID)).thenReturn(wallet("10.00", 0));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.create(USER_ID, IDEMPOTENCY_KEY,
                        request(null, "9", "100.00", null)));

        assertEquals(40003, ex.getCode());
        verify(walletMapper, never()).updateBalance(anyLong(), any(), anyInt());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("转账失败：未填写支付密码返回 40005")
    void createRequiresPaymentPassword() {
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(null);
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));
        when(userService.hasPaymentPassword(USER_ID)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.create(USER_ID, IDEMPOTENCY_KEY,
                        request(null, "9", "100.00", null)));

        assertEquals(40005, ex.getCode());
        verify(userService, never()).verifyPaymentPassword(anyLong(), any());
    }

    @Test
    @DisplayName("转账失败：银行卡余额不足返回 40003")
    void createRejectsInsufficientCardBalance() {
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(null);
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));
        when(userService.hasPaymentPassword(USER_ID)).thenReturn(false);
        when(walletCardMapper.findByIdAndUserId(3L, USER_ID)).thenReturn(card("50.00", "CNY"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.create(USER_ID, IDEMPOTENCY_KEY,
                        request("3", "9", "100.00", null)));

        assertEquals(40003, ex.getCode());
        verify(walletCardMapper, never()).deductBalance(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("转账成功：扣减零钱、写流水并发送通知")
    void createTransferSuccess() {
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(null);
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));
        when(userService.hasPaymentPassword(USER_ID)).thenReturn(false);
        when(walletMapper.findByIdAndUserIdForUpdate(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));
        when(recipientMapper.findByIdAndUserId(9L, USER_ID)).thenReturn(recipient());
        when(walletMapper.updateBalance(eq(WALLET_ID), eq(new BigDecimal("899.90")), eq(0))).thenReturn(1);
        when(transactionMapper.insert(any())).thenAnswer(invocation -> {
            invocation.<Transaction>getArgument(0).setId(55L);
            return 1;
        });
        Transaction saved = transaction(55L);
        when(transactionMapper.findByIdAndUserId(55L, USER_ID)).thenReturn(saved);
        when(transactionService.receipt(USER_ID, 55L)).thenReturn(mock(ReceiptResponse.class));

        TransferResponse response = transferService.create(USER_ID, IDEMPOTENCY_KEY,
                request(null, "9", "100.00", "lunch"));

        assertEquals("55", response.id());
        assertEquals("100.10", response.totalAmount());
        assertEquals("Ann", response.recipient().name());
        verify(walletMapper).updateBalance(WALLET_ID, new BigDecimal("899.90"), 0);
        verify(recipientMapper).touchLastUsed(eq(9L), any());
        verify(notificationService).create(eq(USER_ID), eq("transfer_sent"), any(), any(), any(), any(), eq(55L));
    }

    @Test
    @DisplayName("转账幂等：相同 Idempotency-Key 直接返回首次结果，不重复扣款")
    void createIsIdempotent() {
        Transaction existing = transaction(55L);
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(existing);
        when(transactionService.receipt(USER_ID, 55L)).thenReturn(mock(ReceiptResponse.class));

        TransferResponse response = transferService.create(USER_ID, IDEMPOTENCY_KEY,
                request(null, "9", "100.00", null));

        assertEquals("55", response.id());
        verify(walletMapper, never()).updateBalance(anyLong(), any(), anyInt());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("转账幂等：并发写入触发唯一键冲突时返回已有流水")
    void createFallsBackToExistingOnDuplicateKey() {
        Transaction existing = transaction(55L);
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                .thenReturn(null, existing);
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));
        when(userService.hasPaymentPassword(USER_ID)).thenReturn(false);
        when(walletMapper.findByIdAndUserIdForUpdate(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 0));
        when(recipientMapper.findByIdAndUserId(9L, USER_ID)).thenReturn(recipient());
        when(walletMapper.updateBalance(eq(WALLET_ID), any(), anyInt())).thenReturn(1);
        when(transactionMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate idempotency key"));
        when(transactionService.receipt(USER_ID, 55L)).thenReturn(mock(ReceiptResponse.class));

        TransferResponse response = transferService.create(USER_ID, IDEMPOTENCY_KEY,
                request(null, "9", "100.00", null));

        assertEquals("55", response.id());
    }

    @Test
    @DisplayName("转账失败：余额版本冲突返回 40906")
    void createDetectsOptimisticLockConflict() {
        when(transactionMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(null);
        when(walletMapper.findByIdAndUserId(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 3));
        when(userService.hasPaymentPassword(USER_ID)).thenReturn(false);
        when(walletMapper.findByIdAndUserIdForUpdate(WALLET_ID, USER_ID)).thenReturn(wallet("1000.00", 3));
        when(recipientMapper.findByIdAndUserId(9L, USER_ID)).thenReturn(recipient());
        when(walletMapper.updateBalance(eq(WALLET_ID), any(), eq(3))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                transferService.create(USER_ID, IDEMPOTENCY_KEY,
                        request(null, "9", "100.00", null)));

        assertEquals(40906, ex.getCode());
        verify(transactionMapper, never()).insert(any());
    }

    private CreateTransferRequest request(String cardId, String recipientId, String amount, String note) {
        return new CreateTransferRequest("1", cardId, recipientId, null, amount, note, null);
    }

    private Wallet wallet(String balance, int version) {
        Wallet wallet = new Wallet();
        wallet.setId(WALLET_ID);
        wallet.setUserId(USER_ID);
        wallet.setCurrencyCode("CNY");
        wallet.setBalance(new BigDecimal(balance));
        wallet.setStatus("active");
        wallet.setVersion(version);
        return wallet;
    }

    private WalletCard card(String balance, String currency) {
        WalletCard card = new WalletCard();
        card.setId(3L);
        card.setUserId(USER_ID);
        card.setBalance(new BigDecimal(balance));
        card.setCurrencyCode(currency);
        card.setStatus("active");
        return card;
    }

    private Recipient recipient() {
        Recipient recipient = new Recipient();
        recipient.setId(9L);
        recipient.setUserId(USER_ID);
        recipient.setDisplayName("Ann");
        recipient.setAccountNo("6222000011112222");
        return recipient;
    }

    private Transaction transaction(Long id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setReferenceNo("TR-00000000000000000001");
        transaction.setUserId(USER_ID);
        transaction.setWalletId(WALLET_ID);
        transaction.setType("transfer_out");
        transaction.setDirection("debit");
        transaction.setStatus("success");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setFeeAmount(new BigDecimal("0.10"));
        transaction.setCurrencyCode("CNY");
        transaction.setRecipientId(9L);
        transaction.setCounterpartyName("Ann");
        transaction.setCounterpartyAccountNo("6222000011112222");
        transaction.setIdempotencyKey(IDEMPOTENCY_KEY);
        transaction.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
        return transaction;
    }
}
