package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class ControllerA {

    @Autowired
    ControllerB controllerB = new ControllerB();

    @GetMapping("/test")
    public String callServiceB() {
        User user = new User();
        user.setId(1L);
        user.setUsername("hihi");
        ArrayList<User> userList = new ArrayList<>();

        userList.add(user);
        userList.add(new User());
        User userx = controllerB.test(userList, 1);
        return userx.getUsername();
    }

}
