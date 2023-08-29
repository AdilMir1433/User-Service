package com.example.users.utilities;

import com.example.users.entities.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;


// CLASS TO STORE USER-SPECIFIC DATA IN SESSION
@Component
//@SessionScope
@NoArgsConstructor
@Setter
@Getter
public class SessionData implements Serializable {

    User user;
    public SessionData(User user){
        this.user = user;
    }
    public void removeSessionData(){
        this.user = null;
    }
}

