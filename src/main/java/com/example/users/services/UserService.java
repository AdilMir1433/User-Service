package com.example.users.services;

import com.cloudinary.Cloudinary;
import com.common.Exception.InternalException;
import com.common.Exception.ResourceNotFoundException;
import com.common.QuestionDTOs.QuestionAndScore;
import com.example.users.email.EmailSender;
import com.example.users.entities.Score;
import com.example.users.entities.User;
import com.common.UserDTOs.Roles;
import com.example.users.respositories.ScoreRepository;
import com.example.users.respositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ScoreRepository scoreRepository;
    private final EmailSender emailSender;
    private final Cloudinary cloudinary;

    /** Method to load user details by name
     * @param username: username of the user to be loaded from the database (email) (String)
     * @return User: user details (User)
     * */
    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }


    /** Method to get All users from database
     * @return List<User>: list of all users (List<User>)
     * @throws NoSuchElementException: if no users are found in the database
     * */
    public List<User> getUsers(){
        try {
            return userRepository.findAll();
        }
        catch (Exception e){
            throw new NoSuchElementException("No users found");
        }

    }


    /** Method to save user into database
     * @param user: user details to be saved (User)
     * @return User: saved user details (User)
     * @throws InternalException: if unable to save user
     * */

    @Transactional
    public User save(User user){
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword())); // encode the password
            return userRepository.save(user);
        }
        catch (Exception e){
            throw new InternalException("Unable to save user");
        }
    }



    /** Method to get All teachers under a particular admin
     * @param id: admin id (Long) (id is the id of the admin under which we want to get all the teachers) (id is the id of the admin under which we want to get all the students)
     * @return List<User>: list of all teachers (List<User>)
     * @throws ResourceNotFoundException: if no teachers are found in the database
     * */
    public List<User> getAllTeachers(Long id){
        try {
            List<User> teachers = new ArrayList<>();
            for (User user : userRepository.findAll()) {
                if (user.getRole().equals(Roles.TEACHER) && user.getAdminID().equals(id)) {
                    teachers.add(user);
                }
            }
            return teachers;
        }
        catch (Exception e){
            throw new ResourceNotFoundException("No teachers found");
        }
    }


    /** Method to get All students under a particular admin
     * @param id: admin id (Long) (id is the id of the admin under which we want to get all the students)
     * @return List<User>: list of all students (List<User>)
     * @throws ResourceNotFoundException: if no students are found in the database
     * */
    public List<User> getAllStudents(Long id){
        List<User> students = new ArrayList<>();
        for(User user: userRepository.findAll()){
            if(user.getRole().equals(Roles.STUDENT) && user.getAdminID().equals(id))
            {
                students.add(user);
            }
        }
        return students;
    }
    /** Method to send email to the user
     * @param name: name of the user (String)
     * @param email: email of the user (String)
     * */
    public void emailSender(String name, String email, String password ){
        try {
            String body = "Greetings " + name + "\nA very warm welcome to devXam. Following are your credentials for logIn\nEmail: " + email + "\nPassword: " + password + "\n\nRegards\nDevXam Team";
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Welcome to DevXam!!");
            message.setText(body);
            emailSender.getJavaMailSender().send(message);
        }
        catch (Exception e){
            throw new InternalException("Unable to send email");
        }
    }

    /** Method to get adminID of the user
     * @param id: name of the user (Long)
     * @return Long: adminID of the user (Long)
     * */
    public Long getAdminID(Long id){
        try {
            return userRepository.findById(id).get().getAdminID();
        }
        catch (Exception e){
            throw new ResourceNotFoundException("No admin found");
        }
    }

    /** Method to save score of the user
     * @param score: score of the user (Score)
     * @return void
     * */
    @Transactional
    public void saveScore(Score score)
    {
        try {
            scoreRepository.save(score);
        }
        catch (Exception e){
            throw new InternalException("Unable to save score");
        }
    }

    /** Method to get score of the user
     * @param id: id of the user (Long)
     * @return List<Score>: list of scores of the user (List<Score>)
     * @throws ResourceNotFoundException: if no scores are found in the database
     * */
    public List<Score> getScores(Long id){
        try {
            List<Score> scores = new ArrayList<>();
            for (Score score : scoreRepository.findAll()) {
                if (score.getStudentID().equals(id)) {
                    scores.add(score);
                }
            }
            return scores;
        }
        catch (Exception e){
            throw new ResourceNotFoundException("No scores found");
        }
    }

    /** Method to get score of a question
     * @param questionID: id of the question (Long)
     * @return int: score of the question (int)
     * @throws ResourceNotFoundException: if no score is found in the database
     * */
    public int getQuestionScore(Long questionID) {
        try {
            return scoreRepository.findById(questionID).get().getScore();
        }
        catch (Exception e){
            throw new ResourceNotFoundException("No score found");
        }
    }

    /** Method to get question and its score
     * @param examID: id of the exam (Long)
     * @param studentID: id of the student (Long)
     * @return List<QuestionAndScore>: list of questions and scores of the user (List<QuestionAndScore>)
     * @throws ResourceNotFoundException: if no questions and scores are found in the database
     * */
    public List<QuestionAndScore> getQuestionsAndScoreByExamID(Long examID, Long studentID){
        try {
            List<QuestionAndScore> questionAndScores = new ArrayList<>();
            for (Score score : scoreRepository.findAll()) {
                if (score.getExamID().equals(examID) && score.getStudentID().equals(studentID)) {
                    QuestionAndScore questionAndScore = new QuestionAndScore();
                    questionAndScore.setScore(score.getScore());
                    questionAndScores.add(questionAndScore);
                }
            }
            return questionAndScores;
        }
        catch (Exception e){
            throw new ResourceNotFoundException("No questions found");
        }
    }

    /** Method to get exams of a student
     * @param studentID: id of the student (Long)
     * @return List<Long>: list of exams of the student (List<Long>)
     * @throws ResourceNotFoundException: if no exams are found in the database
     * */
    public List<Long> getExamsOfStudent(Long studentID){
        try {
            List<Long> examDTOIDS = new ArrayList<>();
            for (Score score : scoreRepository.findAll()) {
                if (score.getStudentID().equals(studentID) && !examDTOIDS.contains(score.getExamID())) {
                    examDTOIDS.add(score.getExamID());
                }
            }
            return examDTOIDS;
        }
        catch (Exception e){
            throw new ResourceNotFoundException("Student has not given any exam");
        }
    }

    /** Method to get total score of an exam of a student
     * @param examId: id of the exam (Long)
     * @param userID: id of the student (Long)
     * @return int: total score of the student (int)
     * @throws ResourceNotFoundException: if no score is found in the database
     * */
    public int getTotal(Long examId, Long userID)
    {
        try {
            int total = 0;
            for (Score score : scoreRepository.findAll()) {
                if (score.getExamID().equals(examId) && score.getStudentID().equals(userID)) {
                    total += score.getScore();
                }
            }
            return total;
        }
        catch (Exception e){
            throw new IllegalArgumentException("No score found");
        }
    }
    public byte[] getImageFromCloud(String publicID){

        String cloudUrl = cloudinary.url()
                .publicId(publicID)
                .generate();
        try {
            // Get a ByteArrayResource from the URL
            URL url = new URL(cloudUrl);
            InputStream inputStream = url.openStream();
            byte[] out = org.apache.commons.io.IOUtils.toByteArray(inputStream);
            ByteArrayResource resource = new ByteArrayResource(out);
            return resource.getByteArray();

        } catch (Exception ex) {

            return null;
        }
    }
}
