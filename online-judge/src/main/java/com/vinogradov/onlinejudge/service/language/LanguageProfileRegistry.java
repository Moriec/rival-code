package com.vinogradov.onlinejudge.service.language;

import com.vinogradov.contracts.submissions.enums.ProgrammingLanguages;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class LanguageProfileRegistry {

    private final Map<ProgrammingLanguages, LanguageProfile> profiles = new EnumMap<>(ProgrammingLanguages.class);

    public LanguageProfileRegistry(List<LanguageProfile> profileList) {
        for (LanguageProfile profile : profileList) {
            profiles.put(profile.language(), profile);
        }
    }

    public LanguageProfile getProfile(ProgrammingLanguages language) {
        LanguageProfile profile = profiles.get(language);
        if (profile == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return profile;
    }
}
