package com.example.LiveHost.common.config;

import com.example.LiveHost.common.exception.BusinessException;
import com.example.LiveHost.common.exception.ErrorCode;
import com.example.LiveHost.others.entity.Member;
import com.example.LiveHost.others.entity.Seller;
import com.example.LiveHost.others.repository.MemberRepository;
import com.example.LiveHost.others.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveAuthUtils {

    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;

    /**
     * 현재 로그인한 일반 회원(Member) 반환
     */
    @Transactional(readOnly = true)
    public Member getCurrentMember() {
        String loginId = getCurrentUserLoginId();

        Member member = memberRepository.findByLoginId(loginId);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    /**
     * 현재 로그인한 판매자(Seller) 반환
     */
    @Transactional(readOnly = true)
    public Seller getCurrentSeller() {
        String loginId = getCurrentUserLoginId();

        Seller seller = sellerRepository.findByLoginId(loginId);
        if (seller == null) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }
        return seller;
    }

    /**
     * SecurityContext에서 사용자 로그인 ID(loginId) 추출
     */
    private String getCurrentUserLoginId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_MEMBER);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        return principal.toString();
    }
}
