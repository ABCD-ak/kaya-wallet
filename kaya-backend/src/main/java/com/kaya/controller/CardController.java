package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.card.CardResponse;
import com.kaya.dto.card.CreateCardRequest;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public ApiResponse<List<CardResponse>> list(
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(cardService.list(principal.id(), walletId, status));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CardResponse> create(
            @Valid @RequestBody CreateCardRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(cardService.create(principal.id(), request));
    }

    @DeleteMapping("/{cardId}")
    public ApiResponse<Void> remove(
            @PathVariable Long cardId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        cardService.remove(principal.id(), cardId);
        return ApiResponse.ok(null);
    }
}
