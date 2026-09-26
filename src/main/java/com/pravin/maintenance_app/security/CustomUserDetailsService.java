package com.pravin.maintenance_app.security;

import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String mobileNumber)
                        throws UsernameNotFoundException {

                User user = userRepository.findByMobileNumber(mobileNumber)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found with mobile number: " + mobileNumber));

                return org.springframework.security.core.userdetails.User
                                .withUsername(user.getMobileNumber())
                                .password(user.getPassword())
                                .roles(user.getRole().name())
                                .disabled(user.getStatus() != com.pravin.maintenance_app.ENUM.UserStatus.ACTIVE)
                                .build();
        }
}