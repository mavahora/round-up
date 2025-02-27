package com.starling.roundup.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StarlingAccount {

        private String accountUid;
        private String accountType;
        private String defaultCategory;
        private String currency;
        private String createdAt;
        private String name;
}