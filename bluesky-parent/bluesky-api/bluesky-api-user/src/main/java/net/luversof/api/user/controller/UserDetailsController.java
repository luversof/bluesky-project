package net.luversof.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 계정
 *
 * <p>Spring Security UserDetails 계정. UserDetailsManager 빈이 있을 때만 등록된다.
 */
@RestController
@RequestMapping(value = "/api/userDetails", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnBean(UserDetailsManager.class)
public class UserDetailsController {

  private UserDetailsManager userDetailsManager;

  @Autowired
  public void setUserDetailsManager(UserDetailsManager userDetailsManager) {
    this.userDetailsManager = userDetailsManager;
  }

  /**
   * username으로 계정 조회
   *
   * @param username 계정 이름
   * @return 계정(없으면 UsernameNotFoundException)
   */
  @GetMapping("/search/loadUserByUsername")
  public UserDetails loadUserByUsername(String username) {
    return userDetailsManager.loadUserByUsername(username);
  }

  /**
   * 계정 생성
   *
   * @param user 만들 계정
   * @return 만든 계정
   */
  @PostMapping
  public UserDetails createUser(@RequestBody User user) {
    userDetailsManager.createUser(user);
    return user;
  }

  /**
   * 계정 수정
   *
   * @param user 고칠 계정
   * @return 고친 계정
   */
  @PutMapping
  public UserDetails updateUser(@RequestBody User user) {
    userDetailsManager.updateUser(user);
    return user;
  }

  /**
   * 계정 삭제
   *
   * @param username 지울 계정 이름
   */
  @DeleteMapping
  public void deleteUser(String username) {
    userDetailsManager.deleteUser(username);
  }
}
