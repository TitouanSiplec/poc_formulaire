package com.data.backend.back_end.repository;

import com.data.backend.back_end.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    
}
