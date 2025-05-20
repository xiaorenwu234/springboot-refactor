package com.example.demo;

import com.example.demo.util.LogQualifiedName;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ControllerB {


    @LogQualifiedName
    public User test(ArrayList<User> userList,int i) {
        User user = new User();
        user.setId(1L);
        user.setUsername("okok");
        userList.add(user);
        return user;
    }
}
