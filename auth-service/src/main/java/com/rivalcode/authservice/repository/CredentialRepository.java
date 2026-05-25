package com.rivalcode.authservice.repository;

import com.rivalcode.authservice.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
}