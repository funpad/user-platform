package org.geektimes.projects.user.service.impl;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.repository.DatabaseUserRepository;
import org.geektimes.projects.user.repository.InMemoryUserRepository;
import org.geektimes.projects.user.repository.UserRepository;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.projects.user.sql.DBConnectionManager;

import java.util.Collection;

public class UserServiceImpl implements UserService {

    private UserRepository userRepository = new DatabaseUserRepository(new DBConnectionManager());
    // private UserRepository userRepository = new InMemoryUserRepository();

    /**
     * 注册用户
     *
     * @param user 用户对象
     * @return 成功返回<code>true</code>
     */
    @Override
    public boolean register(User user) {
        if (user == null) {
            return false;
        }
        return userRepository.save(user);
    }

    /**
     * 注销用户
     *
     * @param user 用户对象
     * @return 成功返回<code>true</code>
     */
    @Override
    public boolean deregister(User user) {
        return false;
    }

    /**
     * 更新用户信息
     *
     * @param user 用户对象
     * @return
     */
    @Override
    public boolean update(User user) {
        return false;
    }

    @Override
    public User queryUserById(Long id) {
        return null;
    }

    @Override
    public User queryUserByNameAndPassword(String name, String password) {
        return null;
    }

    @Override
    public Collection<User> getAll() {
        return userRepository.getAll();
    }
}
