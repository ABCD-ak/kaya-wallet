package com.kaya.service;

import com.kaya.dto.wallet.WalletResponse;
import com.kaya.dto.wallet.WalletSummaryResponse;

import java.util.List;

public interface WalletService {

    List<WalletResponse> list(Long userId);

    WalletSummaryResponse summary(Long userId, Long walletId, String month);
}
