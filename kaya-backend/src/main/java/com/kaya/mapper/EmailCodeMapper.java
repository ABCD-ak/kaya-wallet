package com.kaya.mapper;

import com.kaya.entity.EmailCode;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface EmailCodeMapper {

    int insert(EmailCode code);

    EmailCode findLatest(@Param("email") String email, @Param("purpose") String purpose);

    int markUsed(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);

    int incrementAttempts(@Param("id") Long id);

    int countSince(
            @Param("email") String email,
            @Param("purpose") String purpose,
            @Param("since") LocalDateTime since);
}
