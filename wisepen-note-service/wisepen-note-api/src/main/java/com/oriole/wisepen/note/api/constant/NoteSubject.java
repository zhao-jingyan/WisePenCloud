package com.oriole.wisepen.note.api.constant;

import com.oriole.wisepen.common.core.domain.IBusinessSubject;

import java.util.Locale;

public enum NoteSubject implements IBusinessSubject {
    NOTE;

    @Override
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}