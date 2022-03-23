/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.security.auth.mfa.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.service.security.auth.mfa.config.provider.TwoFactorAuthProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;

@Data
@ApiModel
public class TwoFactorAuthSettings {

    @ApiModelProperty(value = "Option for tenant admins to use 2FA settings configured by sysadmin. " +
            "If this param is set to true, then the settings will not be validated for constraints " +
            "(if it is a tenant admin; for sysadmin this param is ignored)")
    private boolean useSystemTwoFactorAuthSettings;
    @ApiModelProperty(value = "The list of 2FA providers' configs. Users will only be allowed to use 2FA providers from this list.")
    @Valid
    private List<TwoFactorAuthProviderConfig> providers;

    @ApiModelProperty(value = "Rate limit configuration for verification code sending. The format is standard: 'amountOfRequests:periodInSeconds'. " +
            "The value of '1:60' would limit verification code sending requests to one per minute.", example = "1:60", required = false)
    @Pattern(regexp = "[1-9]\\d*:[1-9]\\d*", message = "verification code send rate limit configuration is invalid")
    private String verificationCodeSendRateLimit;
    @ApiModelProperty(value = "Rate limit configuration for verification code checking.", example = "3:900", required = false)
    @Pattern(regexp = "[1-9]\\d*:[1-9]\\d*", message = "verification code check rate limit configuration is invalid")
    private String verificationCodeCheckRateLimit;
    @ApiModelProperty(value = "Maximum number of verification failures before a user gets disabled.", example = "10", required = false)
    @Min(value = 0, message = "maximum number of verification failure before user lockout must be positive")
    private int maxVerificationFailuresBeforeUserLockout;
    @ApiModelProperty(value = "Total amount of time in seconds allotted for verification. " +
            "Basically, this property sets a lifetime for pre-verification token. If not set, default value of 30 minutes is used.", example = "3600", required = false)
    @Min(value = 1, message = "total amount of time allotted for verification must be greater than 0")
    private Integer totalAllowedTimeForVerification;


    public Optional<TwoFactorAuthProviderConfig> getProviderConfig(TwoFactorAuthProviderType providerType) {
        return Optional.ofNullable(providers)
                .flatMap(providersConfigs -> providersConfigs.stream()
                        .filter(providerConfig -> providerConfig.getProviderType() == providerType)
                        .findFirst());
    }

}
