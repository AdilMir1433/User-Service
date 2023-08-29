package com.example.users.controllers;

import com.cloudinary.Cloudinary;
import com.common.ExamDTOs.ScoreDTO;
import com.common.JWTDTOs.JWTRequest;
import com.common.QuestionDTOs.QuestionAndScore;
import com.common.UserDTOs.Roles;
import com.common.UserDTOs.UserDTO;
import com.common.UserDTOs.UserDTOSession;
import com.example.users.email.EmailSender;
import com.example.users.entities.Score;
import com.example.users.entities.User;
import com.example.users.model.SaveToken;
import com.example.users.security.JWTUtility;
import com.example.users.services.UserService;
import com.example.users.utilities.SessionData;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.common.Image.ImageCompressor.compressImage;

@Slf4j
@RequestMapping("/users/authentication")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserDetailsService userDetailsService;
    private final Cloudinary cloudinary;
    private final EmailSender emailSender;
    private final AuthenticationManager manager;
    private final JWTUtility helper;
    private final UserService userService;
    private final SessionData sessionData;
    private final ModelMapper modelMapper;


    /** Method that validates user and then redirects the user according to their appropriate roles
     * @param request
     * @return String
     * @throws Exception
     * @throws BadCredentialsException
     * @throws IOException
     * */
    @PostMapping("/login")
    public String login(@RequestBody JWTRequest request) {
        sessionData.setUser(null);
        User user = userService.loadUserByUsername(request.getEmail());
        UserDetails userDetails = userService.loadUserByUsername(request.getEmail());
        try {
            if (helper.validateToken(user.getToken(), user)) { // IF TOKEN IS VALID (NOT EXPIRED)
                this.doAuthenticate(user.getToken(), userDetails);
                sessionData.setUser(user);
                log.info("VALIDATED");
                if (user.getRole().equals(Roles.STUDENT)) return "redirect:/ui/student-dashboard";
                else if (user.getRole().equals(Roles.TEACHER)) return "redirect:/ui/teacher-dashboard";
                return "redirect:/ui/welcome";
            }
        } catch (Exception e) {
            log.info("EXPIRED");

            String token = this.helper.generateToken(user);
            user.setToken(token);
            userService.save(user);
            sessionData.setUser(user);

            log.info("TOKEN generated in AUTHController: {}", token);

            String body = "Greetings\nThis is Personal Access Token. Paste it into website and NEVER Share it with anyone\n\n" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.getEmail());
            message.setSubject("Your Personal Access Token For DevXam");
            message.setText(body);

            emailSender.getJavaMailSender().send(message);

            return "redirect:/ui/verify-token";
        }
        return "redirect:/ui/verify-token";
    }


    /** Method that authenticates the user
     * @param token: String - token of the user
     * @param userDetails: UserDetails - details of the user
     * */
    private void doAuthenticate(String token, UserDetails userDetails) {

        try {
            if(helper.validateToken(token, userDetails))
                log.info("AUTHENTICATE : ");
            else {
                throw new BadCredentialsException("Bad Credentials");
            }
        }
        catch (BadCredentialsException e)
        {
            throw new BadCredentialsException(" Invalid Username or Password  !!");
        }

    }


    /** Method to create a new user account
     * @param user: UserDTO - user to be created
     * @return String
     * */

    @PostMapping("/create-user")
    public String createUser(@RequestBody UserDTO user){

        User newUser = modelMapper.map(user, User.class);
        String token = this.helper.generateToken(newUser);
        newUser.setToken(token);
        newUser.setRole(Roles.ADMIN);
        sessionData.setUser(newUser);
        userService.save(newUser);
        log.info("TOKEN generated in AUTH Controller SignUP : {}", token);

        String body = "Greetings\nThis is Personal Access Token. Paste it into website and NEVER Share it with anyone\n" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Your Personal Access Token For DevXam");
        message.setText(body);
        emailSender.getJavaMailSender().send(message);

        return "redirect:/ui/verify-token";
    }


    /** Method that redirects user according to their role after validating the token
     * @param token: String - token of the user
     * @return String
     * */

    @PostMapping("/verification")
    public String verifyToken(@ModelAttribute("token") String token) {
        SaveToken saveToken = SaveToken.getInstance();
        saveToken.setToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(helper.getUsernameFromToken(token));
        User user = userService.loadUserByUsername(helper.getUsernameFromToken(token));
        user.setToken(token);
        sessionData.setUser(user);
        userService.save(user);

        // Validate the token against the UserDetails retrieved from the database
        if (helper.validateToken(token, userDetails)) {
            if(user.getRole().equals(Roles.STUDENT))
            {
                return "redirect:/ui/student-dashboard";
            }
            else if(user.getRole().equals(Roles.TEACHER))
            {
                return "redirect:/ui/teacher-dashboard";
            }
            else
            {
                return "redirect:/ui/welcome";
            }
        }
        else
        {
            // Token is invalid or expired, handle accordingly
            return "redirect:/ui/login";
        }
    }


    /** Method to save student
     * @param name: String - name of the student
     * @param email: String - email of the student
     * @param password: String - password of the student
     * @param image: MultipartFile - image of the student
     * @return String
     * */
    @PostMapping("/save-student")
    public String saveStudent(@RequestParam("name") String name, @RequestParam("email") String email, @RequestParam("password") String password, @RequestParam("displayPicture") MultipartFile image) throws IOException {
        User user = new User();
        user.setPassword(password);
        user.setEmail(email);
        user.setName(name);
        user.setRole(Roles.STUDENT);
        user.setToken(helper.generateToken(user));

        log.info("Admin ID : " + sessionData.getUser().getUserID());

        user.setAdminID(sessionData.getUser().getUserID());
        Map<String, String> uploadResult = cloudinary.uploader().upload(image.getBytes(), Map.of());
        String publicId = uploadResult.get("public_id");
        user.setDisplayPicture(publicId);
        userService.save(user);
        userService.emailSender(user.getName(), user.getEmail(), password);
        return "redirect:/ui/welcome";
    }

    /** Method to save teacher
     * @param userDTO: UserDTO - user to be saved
     * @return String
     * */
    @PostMapping("/save-teacher")
    public String saveTeacher(@RequestBody UserDTO userDTO){
        User user = modelMapper.map(userDTO, User.class);
        user.setRole(Roles.TEACHER);
        user.setToken(helper.generateToken(user));
        user.setAdminID(sessionData.getUser().getUserID());
        userService.save(user);
        userService.emailSender(user.getName(), user.getEmail(), user.getPassword());
        return "redirect:/ui/welcome";
    }

    /** Method to return current user data
     * @return UserDTOSession
     * */
    @GetMapping("/sessionData")
    public UserDTOSession getSessionData() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(sessionData.getUser().getUserID());
        userDTO.setName(sessionData.getUser().getName());
        userDTO.setEmail(sessionData.getUser().getEmail());
        userDTO.setRole(sessionData.getUser().getRole());
        userDTO.setAdminID(sessionData.getUser().getAdminID());
        String imageID = sessionData.getUser().getDisplayPicture();
        userDTO.setDisplayPicture(userService.getImageFromCloud(imageID));
        UserDTOSession userDTOSession = new UserDTOSession();
        userDTOSession.setUser(userDTO);
        return userDTOSession;
    }

    /** Method to return all teachers created by a particular admin
     * @param id: Long - id of the admin
     * @return List<UserDTO>
     * */

    @PostMapping("/get-teacher-id")
    List<Long> getTeacherId(@RequestParam("id") Long id){
        List<Long> ids = new ArrayList<>();
        List<User> users = userService.getAllTeachers(id);
        for(User user : users){
            ids.add(user.getUserID());
        }
        return ids;
    }

    /** Method to return admin id of a particular user
     * @param id: Long - id of the admin
     * @return List<UserDTO>
     * */
    @PostMapping("/get-admin-id")
    Long getAdminId(@RequestParam("id") Long id){
     return userService.getAdminID(id);
    }
    @PostMapping("/save-score")
    public void saveScore(@RequestBody ScoreDTO scoreDTO){
        Score score = modelMapper.map(scoreDTO, Score.class);
        userService.saveScore(score);
    }

    /** Method to return all students
     * @return List<UserDTO>
     * */
    @GetMapping("/get-all-students")
    public List<UserDTO> getAllStudents(){
        List<UserDTO> userDTOS = new ArrayList<>();
        List<User> users = userService.getUsers();
        for(User user : users){
            if(user.getRole().equals(Roles.STUDENT)){
                UserDTO userDTO = new UserDTO();
                userDTO.setAdminID(user.getAdminID());
                userDTO.setId(user.getUserID());
                userDTO.setName(user.getName());
                userDTO.setEmail(user.getEmail());
                userDTO.setRole(user.getRole());
                String imageID = user.getDisplayPicture();
                userDTO.setDisplayPicture(userService.getImageFromCloud(imageID));
                userDTOS.add(userDTO);
            }
        }
        return userDTOS;
    }

    /** Method to return score of a student
     * @param id: Long - id of the student
     * @return List<UserDTO>
     * */

    @GetMapping("/get-score-of-student")
    public List<ScoreDTO> getScoreOfStudent(@RequestParam("id") Long id){
        List<ScoreDTO> scoreDTOS = new ArrayList<>();
        List<Score> scores = userService.getScores(id);
        for(Score score : scores){
            ScoreDTO scoreDTO = modelMapper.map(score, ScoreDTO.class);
            scoreDTOS.add(scoreDTO);
        }
        return scoreDTOS;
    }

    /** Method to get score of a particular question
     * @param questionID: Long - id of the question
     * @return int
     * */

    @GetMapping("/get-question-score")
    public int questionScore(@RequestParam Long questionID){
        return userService.getQuestionScore(questionID);
    }

    /** Method to get score of a particular question
     * @param examID: Long - id of the exam
     * @param studentID: Long - id of the student
     * @return List<QuestionAndScore>
     * */
    @GetMapping("/get-scores-and-questions")
    public List<QuestionAndScore> getQuestionsAndScoreByExamID(@RequestParam Long examID, @RequestParam Long studentID)
    {
        return userService.getQuestionsAndScoreByExamID(examID, studentID);
    }

    /** Method to get all exams attempted by a student
     * @param studentID: Long - id of the student
     * @return List<Long>
     * */

    @GetMapping("/get-exams-of-student")
    public List<Long> getExamsOfStudent(@RequestParam Long studentID){
        return userService.getExamsOfStudent(studentID);
    }

    /** Method to get total score of a student
     * @param examId: Long - id of the exam
     * @param userID: Long - id of the student
     * @return int
     * */
    @GetMapping("/get-total-of-student")
    public int getTotal(@RequestParam Long examId, @RequestParam Long userID){
        return userService.getTotal(examId, userID);
    }

}
