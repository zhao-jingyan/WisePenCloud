package com.oriole.wisepen.common.core.domain;

import com.oriole.wisepen.common.core.domain.enums.BusinessDomain;

public class ResultKey {

    private final BusinessDomain domain;
    private final IBusinessSubject subject;
    private final IResultOutcome outcome;

    public ResultKey(BusinessDomain domain, IBusinessSubject subject, IResultOutcome outcome) {
        this.domain = domain;
        this.subject = subject;
        this.outcome = outcome;
    }

    @Override
    public String toString() {
        return domain.key() + "." + subject.key() + "." + outcome.key();
    }
}
