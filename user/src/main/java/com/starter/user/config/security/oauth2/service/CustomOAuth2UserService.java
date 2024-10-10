package com.starter.user.config.security.oauth2.service;

import com.starter.user.application.dto.SaveClientUserParameterDTO;
import com.starter.user.domain.user.Gender;
import com.starter.user.domain.user.UserRole;
import com.starter.user.domain.user.admin.service.AdminUserDomainService;
import com.starter.user.domain.user.client.ClientUser;
import com.starter.user.domain.user.client.service.ClientUserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final ClientUserDomainService clientUserDomainService;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> service = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = service.loadUser(userRequest);


        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuthUserInfo oAuthUserInfo = switch (registrationId) {
            case "google" -> this.convertGoogleUser(attributes);
            case "kakao" -> this.convertKakaoUser(attributes);
            case "naver" -> this.convertNaverUser(attributes);
            default -> throw new OAuth2AuthenticationException(new OAuth2Error("100"));
        };

        ClientUser clientUser = clientUserDomainService.saveClientUser(SaveClientUserParameterDTO.builder()
                .email(oAuthUserInfo.getEmail())
                .userName(oAuthUserInfo.getName())
                .nickname(oAuthUserInfo.getNickname())
                .userGender(Optional.ofNullable(oAuthUserInfo.getGender())
                    .map(Gender::valueOf)
                    .orElse(null))
                .userBirthday(oAuthUserInfo.getBirthday())
                .phoneNumber(oAuthUserInfo.getPhoneNumber())
            .build());

        Set<GrantedAuthority> authorities = clientUser.getType().getRoles().stream()
            .map(UserRole::getKey)
            .map(role -> (GrantedAuthority) () -> role)
            .collect(Collectors.toSet());

        Map<String, Object> additionalParameters = userRequest.getAdditionalParameters();
        return new CustomOAuth2User(clientUser.getId(), clientUser.getEmail(), authorities, oAuthUserInfo.getRegistrationId(), attributes, additionalParameters);
    }

    private OAuthUserInfo convertGoogleUser(Map<String, Object> attributes) {
        return OAuthUserInfo.builder()
            .registrationId("google")
            .attributes(attributes)
            .name(String.valueOf(attributes.get("family_name")) + attributes.get("given_name"))
            .nickname(String.valueOf(attributes.get("name")))
            .email(String.valueOf(attributes.get("email")))
            .profileImageUrl(String.valueOf(attributes.get("picture")))
            .build();
    }

    private OAuthUserInfo convertKakaoUser(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
        return OAuthUserInfo.builder()
            .registrationId("kakao")
            .attributes(attributes)
            .name(String.valueOf(kakaoProfile.get("nickname")))
            .email(String.valueOf(kakaoAccount.get("email")))
            .gender(String.valueOf(kakaoAccount.get("gender")))
            .profileImageUrl(String.valueOf(kakaoProfile.get("profile_image_url")))
            .build();
    }

    private OAuthUserInfo convertNaverUser(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return OAuthUserInfo.builder()
            .registrationId("naver")
            .attributes(attributes)
            .name(String.valueOf(response.get("nickname")))
            .email(String.valueOf(response.get("email")))
            .profileImageUrl(String.valueOf(response.get("profile_image")))
            .gender(String.valueOf(response.get("gender")))
            .build();
    }

}
