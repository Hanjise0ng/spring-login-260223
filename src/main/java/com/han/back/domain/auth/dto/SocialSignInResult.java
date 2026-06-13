package com.han.back.domain.auth.dto;

public sealed interface SocialSignInResult {

    final class Authenticated implements SocialSignInResult {

        private final SignInResult signInResult;

        private Authenticated(SignInResult signInResult) {
            this.signInResult = signInResult;
        }

        public static Authenticated of(SignInResult signInResult) {
            return new Authenticated(signInResult);
        }

        public SignInResult getSignInResult() {
            return signInResult;
        }
    }

    final class EmailRequired implements SocialSignInResult {

        private final String provider;
        private final String providerId;
        private final String nickname;

        private EmailRequired(String provider, String providerId, String nickname) {
            this.provider = provider;
            this.providerId = providerId;
            this.nickname = nickname;
        }

        public static EmailRequired of(String provider, String providerId, String nickname) {
            return new EmailRequired(provider, providerId, nickname);
        }

        public String getProvider() {
            return provider;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getNickname() {
            return nickname;
        }
    }

    final class LinkSuggested implements SocialSignInResult {

        private final String provider;
        private final String providerId;
        private final String nickname;

        private LinkSuggested(String provider, String providerId, String nickname) {
            this.provider = provider;
            this.providerId = providerId;
            this.nickname = nickname;
        }

        public static LinkSuggested of(String provider, String providerId, String nickname) {
            return new LinkSuggested(provider, providerId, nickname);
        }

        public String getProvider() {
            return provider;
        }

        public String getProviderId() {
            return providerId;
        }

        public String getNickname() {
            return nickname;
        }
    }

}