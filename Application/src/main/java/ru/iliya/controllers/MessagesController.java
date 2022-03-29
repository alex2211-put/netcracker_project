package ru.iliya.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.iliya.entities.Message;
import ru.iliya.entities.User;
import ru.iliya.repositories.MongoRepositoryImpl;
import ru.iliya.security.SecurityUserConverter;
import ru.iliya.services.MessageService;

import org.bson.Document;

import javax.print.Doc;
import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.List;

@Controller
public class MessagesController {

    @Autowired
    MessageService messageService;
    @Autowired
    SecurityUserConverter securityUserConverter;

    public static class LastMessage {
        public User user;
        public String message;
        public String ownerId;

        public LastMessage(User user, String message, String ownerId) {
            this.user = user;
            this.message = message;
            this.ownerId = ownerId;
        }
    }

    public static class OwnerDialog {
        public String ownerId;
        public String partnerId;
        public List<Message> messages;

        public OwnerDialog(String ownerId, List<Message> messages, String partnerId) {
            this.ownerId = ownerId;
            this.messages = messages;
            this.partnerId = partnerId;
        }
    }


    @GetMapping("/user/dialogs")
    public String showAllDialogsForUser(@RequestParam(name = "user", required = false, defaultValue = "1") String userId,
                                        @AuthenticationPrincipal UserDetails currentUser,
                                        Model model) {
        User user = securityUserConverter.getUserByDetails(currentUser);
        List<User> users = messageService.getDialogsForUser(String.valueOf(user.getUserID()));
        List<LastMessage> lastMessages = new ArrayList<>();
        for (User user1 : users) {
            String message = messageService.getLastMessage(userId, user1);
            lastMessages.add(new LastMessage(user1, message, userId));
        }
        model.addAttribute("lastMessages", lastMessages);
        if (!lastMessages.isEmpty()) {
            return "all-dialogs-for-user";
        }
        return "no-dialogs-for-user";
    }

    @RequestMapping("/user/dialogs")
    public String showAllDialogsForUserParam(@AuthenticationPrincipal UserDetails currentUser,
                                             Model model) {
        User user = securityUserConverter.getUserByDetails(currentUser);
        String userId = String.valueOf(user.getUserID());
        List<User> users = messageService.getDialogsForUser(String.valueOf(user.getUserID()));
        List<LastMessage> lastMessages = new ArrayList<>();
        for (User user1 : users) {
            String message = messageService.getLastMessage(userId, user1);
            lastMessages.add(new LastMessage(user1, message, userId));
        }
        model.addAttribute("lastMessages", lastMessages);
        if (!lastMessages.isEmpty()) {
            return "all-dialogs-for-user";
        }
        return "no-dialogs-for-user";
    }

    private List<Message> messages2 = new ArrayList<>();
    String person = null;

    @GetMapping("/user/chat/{person}")
    public String showMessagesForUser(@PathVariable(name = "person") String person,
                                      @AuthenticationPrincipal UserDetails currentUser,
                                      Model model) {
        User user = securityUserConverter.getUserByDetails(currentUser);
        String owner = String.valueOf(user.getUserID());
        List<Document> messages = messageService.getAllMessagesForDialog(owner, person);
        if (this.person == null || !person.equals(owner)) {
            messages2 = new ArrayList<>();
            for (Document document : messages) {
                messages2.add(new Message(
                        document.get("text").toString(),
                        document.get("from").toString(),
                        document.get("to").toString(),
                        "2022"));
            }
            this.person = owner;
        }
        model.addAttribute("ownerDialog", new OwnerDialog(owner, messages2, person));
        return "p2p-dialog";
    }

    @PostMapping("/user/chat/{person}/write")
    public String showAllDialogsForUser(@PathVariable(name = "person") String person,
                                        @RequestParam(name = "message") String message,
                                        @AuthenticationPrincipal UserDetails currentUser,
                                        Model model) {
        User user = securityUserConverter.getUserByDetails(currentUser);
        String owner = String.valueOf(user.getUserID());
        if (message.isEmpty())
            return "redirect:/user/chat/{owner}/{person}";
        Message message1 = new Message(message, owner, person, "2022");
        messageService.writeToUser(message1);
        messages2.add(message1);
        model.addAttribute("ownerDialog", new OwnerDialog(owner, messages2, person));
        return "redirect:/user/chat/{owner}/{person}";
    }

}
