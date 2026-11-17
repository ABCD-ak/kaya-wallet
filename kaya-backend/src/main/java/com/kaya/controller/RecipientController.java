package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.common.PageResult;
import com.kaya.dto.recipient.CreateRecipientRequest;
import com.kaya.dto.recipient.RecipientResponse;
import com.kaya.dto.recipient.UpdateRecipientRequest;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.RecipientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recipients")
public class RecipientController {

    private final RecipientService recipientService;

    public RecipientController(RecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @GetMapping
    public ApiResponse<PageResult<RecipientResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean recent,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(recipientService.list(principal.id(), search, recent, page, pageSize));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecipientResponse> create(
            @Valid @RequestBody CreateRecipientRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(recipientService.create(principal.id(), request));
    }

    @PatchMapping("/{recipientId}")
    public ApiResponse<RecipientResponse> update(
            @PathVariable Long recipientId,
            @Valid @RequestBody UpdateRecipientRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(recipientService.update(principal.id(), recipientId, request));
    }

    @DeleteMapping("/{recipientId}")
    public ApiResponse<Void> remove(
            @PathVariable Long recipientId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        recipientService.remove(principal.id(), recipientId);
        return ApiResponse.ok(null);
    }
}
