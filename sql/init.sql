/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `forms_faculties`
--
SET FOREIGN_KEY_CHECKS=0;

LOCK TABLES `forms_faculties` WRITE;
/*!40000 ALTER TABLE `forms_faculties` DISABLE KEYS */;
INSERT INTO `forms_faculties` VALUES (2,'EDIC','Computer and Communication Sciences',1,'edic-forms@epfl.ch','EDIC','EDIC','edic@epfl.ch');
/*!40000 ALTER TABLE `forms_faculties` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_definitions`
--

LOCK TABLES `forms_definitions` WRITE;
/*!40000 ALTER TABLE `forms_definitions` DISABLE KEYS */;
INSERT INTO `forms_definitions` VALUES (2,2,'edic','EDIC v2','<p class=\"hidden-print\"><em>This report is a <strong>diagnosis tool</strong> meant to review the current thesis work, supervision, and professional relationship between student and supervisor.</em></p>','  <p><em>The joint review must be completed by the student and is then approved and submitted by the thesis director ideally after a \"face-to-face\" meeting with the student. Only the student can edit the document, whereas the thesis director will have the right to approve and submit the document.</em></p>    <p><em>Once the joint review is approved  and submitted , both the student and the thesis director will complete and submit their private review separately.</em></p>'),(3,2,'edic','EDIC v1','<p class=\"hidden-print\"><em>This report is a <strong>diagnosis tool</strong> meant to review the current thesis work, supervision, and professional relationship between student and supervisor.</em></p>','  <p><em>The joint review must be completed by the student and is then approved and submitted by the thesis director ideally after a \"face-to-face\" meeting with the student. Only the student can edit the document, whereas the thesis director will have the right to approve and submit the document.</em></p>    <p><em>Once the joint review is approved  and submitted , both the student and the thesis director will complete and submit their private review separately.</em></p>'),(4,2,'edic','EDIC v3','<p class=\"hidden-print\"><em>This report is a <strong>diagnosis tool</strong> meant to review the current thesis work, supervision, and professional relationship between student and supervisor.</em></p>','  <p><em>The joint review must be completed by the student and is then approved and submitted by the thesis director ideally after a \"face-to-face\" meeting with the student. Only the student can edit the document, whereas the thesis director will have the right to approve and submit the document.</em></p>    <p><em>Once the joint review is approved  and submitted , both the student and the thesis director will complete and submit their private review separately.</em></p>');
/*!40000 ALTER TABLE `forms_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_filters`
--

LOCK TABLES `forms_filters` WRITE;
/*!40000 ALTER TABLE `forms_filters` DISABLE KEYS */;
INSERT INTO `forms_filters` VALUES (1,4,NULL,'director','Director',1),(2,4,68,'question','Grade',2),(3,2,NULL,'director','Director',1),(4,2,44,'question','Grade',2);
/*!40000 ALTER TABLE `forms_filters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_forms`
--

LOCK TABLES `forms_forms` WRITE;
/*!40000 ALTER TABLE `forms_forms` DISABLE KEYS */;
INSERT INTO `forms_forms` VALUES (6,2,3,2011,0,'2011-01-01','2011-12-31'),(7,2,3,2012,0,'2012-01-01','2012-12-31'),(8,2,2,2012,1,'2012-01-01','2012-12-31'),(9,2,2,2013,0,'2013-01-01','2013-12-31'),(10,2,2,2013,1,'2013-01-01','2013-12-31'),(11,2,2,2014,0,'2014-01-01','2014-12-31'),(12,2,2,2014,1,'2014-01-01','2014-12-31'),(13,2,2,2015,0,'2015-01-01','2015-12-31'),(14,2,2,2015,1,'2015-01-01','2015-12-31'),(15,2,4,2016,0,'2016-09-01','2016-12-31'),(16,2,4,2016,1,'2017-01-01','2017-12-31'),(18,2,4,2017,0,'2017-09-01','2017-12-31');
/*!40000 ALTER TABLE `forms_forms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_notifications`
--

LOCK TABLES `forms_notifications` WRITE;
/*!40000 ALTER TABLE `forms_notifications` DISABLE KEYS */;
INSERT INTO `forms_notifications` VALUES (7,4,'joint completed - to student','EPFL - EDIC PhD student annual report - [[student_fullname]] - Joint part completed','Dear [[to_firstname]],\r\n\r\nThe joint part of the form \'[[form_fulltitle]]\' is now complete and validated.\r\n\r\nThanks,'),(8,4,'joint completed - to main director','EPFL - EDIC PhD student annual report - [[student_fullname]] - Please complete your private section !','Dear [[to_firstname]],\n\nThe joint part of the form \'[[form_fulltitle]]\' is now complete and validated.\nPlease now fill the director\'s section of the form.\n\nYou can access the form at the following URL: [[form_url]]\n\n\nThanks,'),(9,4,'joint validated - to director','EPFL - EDIC PhD student annual report - [[student_fullname]] - Joint part needs your validation!','Dear [[to_firstname]],\n\nAs part of the on-line annual report of your PhD student(s) affiliated in the EDIC doctoral program, [[student_fullname]] submitted the joint part of his/her report.\n\nPlease read this report. If you agree with its content, please validate it. Otherwise please discuss the necessary changes with the student.\n\nYou can access the form at the following URL: [[form_url]]\n\nIf you have a co-supervisor for this student, he/she has also received an email with a link to this joint report, but in read-only mode only (no validation required). Please be in touch with the co-supervisor as well to agree on the content of the joint report. When you all agree on the content of this report, please validate it.\n\nThanks,'),(10,4,'joint validated - to codirector','EPFL - EDIC PhD student annual report - [[student_fullname]] - Joint part has been submitted.','Dear [[to_firstname]],\n\nAs part of the on-line annual report of your PhD student(s) affiliated in the EDIC doctoral program of EPFL, the student you co-supervise, [[student_fullname]], submitted the joint part of his/her report.\n\nPlease read this report. If you agree with its content, no action is needed from you. Otherwise please discuss the necessary changes with the student and the main supervisor.\n\nYou can access the form at the following URL: [[form_url]]\n\nThanks,'),(11,4,'form complete - to all','EPFL - EDIC PhD student annual report - [[student_fullname]] - Report is complete!','Dear [[to_firstname]],\r\n\r\nBy this email we want to let you know that all the forms of the EDIC report of [[student_fullname]] have been successfully submitted. Thank you for your contribution.\r\n\r\nYou can access the form at the following URL: [[form_url]]\r\n\r\nThanks,');
/*!40000 ALTER TABLE `forms_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_notifications_to_events`
--

LOCK TABLES `forms_notifications_to_events` WRITE;
/*!40000 ALTER TABLE `forms_notifications_to_events` DISABLE KEYS */;
INSERT INTO `forms_notifications_to_events` VALUES (7,'joint_completed','student'),(8,'joint_completed','director'),(9,'joint_validated','director'),(9,'joint_updated','director'),(10,'joint_validated','codirector'),(10,'joint_updated','codirector'),(11,'form_completed','director'),(11,'form_completed','codirector'),(11,'form_completed','student'),(11,'form_completed','faculty');
/*!40000 ALTER TABLE `forms_notifications_to_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_parts`
--

LOCK TABLES `forms_parts` WRITE;
/*!40000 ALTER TABLE `forms_parts` DISABLE KEYS */;
INSERT INTO `forms_parts` VALUES (8,4,1,'Questions For the Doctoral Student'),(9,5,1,'Questions for the thesis Director/Co-Director'),(10,6,1,'Questions For the Doctoral Student'),(11,7,1,'Questions for the thesis Director/Co-Director'),(12,8,1,'Questions For the Doctoral Student'),(13,9,1,'Questions for the thesis Director/Co-Director');
/*!40000 ALTER TABLE `forms_parts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_questions`
--

LOCK TABLES `forms_questions` WRITE;
/*!40000 ALTER TABLE `forms_questions` DISABLE KEYS */;
INSERT INTO `forms_questions` VALUES (38,2,8,1,'Which course did you take since the last review? (list all grades)',NULL,'free',0),(39,2,8,2,'Which teaching activities did you perform since the last review?',NULL,'free',0),(40,2,8,3,'What research progress did you make since the last review? (List all publications and submissions, if any.)',NULL,'free',0),(41,2,8,4,'What progress do you plan to make between now and the next review?',NULL,'free',0),(42,2,8,5,'Other activities (e.g., demos)',NULL,'free',0),(43,2,8,6,'(Optional) Comments (e.g. request for supervisor\'s actions)',NULL,'free',1),(44,2,9,1,'Overall appraisal of progress:',NULL,'select',0),(45,2,9,2,'What has the student done well since the last review?',NULL,'free',0),(46,2,9,3,'Which difficulties has the student encountered since the last review?',NULL,'free',0),(47,2,9,4,'Which skills most require the student\'s attention and which corrective actions do you propose?',NULL,'free',0),(48,2,9,5,'Which milestones or deadlines have been agreed upon between now and the next review?',NULL,'free',0),(49,2,9,6,'(Optional) Supervisor\'s advice to the student:',NULL,'free',1),(50,3,10,1,'Which course did you take since the last review? (list all grades)',NULL,'free',0),(51,3,10,2,'Which teaching activities did you perform since the last review?',NULL,'free',0),(52,3,10,3,'What research progress did you make since the last review? (List all publications and submissions, if any.)',NULL,'free',0),(53,3,10,4,'What progress do you plan to make between now and the next review?',NULL,'free',0),(54,3,10,5,'Other activities (e.g., demos)',NULL,'free',0),(55,3,10,6,'(Optional) Comments (e.g. request for supervisor\'s actions)',NULL,'free',1),(56,3,11,1,'Overall appraisal of progress:',NULL,'free',0),(57,3,11,2,'What has the student done well since the last review?',NULL,'free',0),(58,3,11,3,'Which difficulties has the student encountered since the last review?',NULL,'free',0),(59,3,11,4,'Which skills most require the student\'s attention and which corrective actions do you propose?',NULL,'free',0),(60,3,11,5,'Which milestones or deadlines have been agreed upon between now and the next review?',NULL,'free',0),(61,3,11,6,'(Optional) Supervisor\'s advice to the student:',NULL,'free',1),(62,4,12,1,'Which depth course did you take so far?',NULL,'coursesgrades',0),(63,4,12,4,'Which teaching activities did you perform since the last review?',NULL,'free',0),(64,4,12,5,'What research progress did you make since the last review? (List all publications and submissions, if any.)',NULL,'free',0),(65,4,12,6,'What progress do you plan to make between now and the next review?',NULL,'free',0),(66,4,12,7,'Other activities (e.g., demos)',NULL,'free',1),(67,4,12,8,'(Optional) Comments (e.g. request for supervisor\'s actions)',NULL,'free',1),(68,4,13,1,'Overall appraisal of progress:',NULL,'select',0),(69,4,13,2,'What has the student done well since the last review?',NULL,'free',0),(70,4,13,3,'Which difficulties has the student encountered since the last review?',NULL,'free',0),(71,4,13,4,'Which skills most require the student\'s attention and which corrective actions do you propose?',NULL,'free',0),(72,4,13,5,'Which milestones or deadlines have been agreed upon between now and the next review?',NULL,'free',0),(73,4,13,6,'(Optional) Supervisor\'s advice to the student:',NULL,'free',1),(75,4,12,2,'Which breadth courses did you take so far?',NULL,'freegrades',1);
/*!40000 ALTER TABLE `forms_questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_questions_choices`
--

LOCK TABLES `forms_questions_choices` WRITE;
/*!40000 ALTER TABLE `forms_questions_choices` DISABLE KEYS */;
INSERT INTO `forms_questions_choices` VALUES (13,44,1,'Unsatisfactory',0),(14,44,2,'Needs improvement',0),(15,44,3,'Meets expectations',0),(16,44,4,'Exceeds expectations',0),(17,68,1,'Unsatisfactory',0),(18,68,2,'Needs improvement',0),(19,68,3,'Meets expectations',0),(20,68,4,'Exceeds expectations',0),(21,62,34,'COM-514 Mathematical Foundations of Signal Processing',0),(22,62,2,'CS-430 Intelligent Agents',0),(23,62,3,'CS-433 Pattern Classification and Machine Learning',0),(24,62,33,'COM-503 Performance Evaluation',0),(25,62,1,'CS-422 Database Systems',0),(26,62,6,'CS-471 Advanced MultiProcessor Architecture',0),(27,62,7,'CS-472 Design Technologies for Integrated Systems',0),(28,62,8,'CS-522  Principles of Computer Systems',0),(29,62,30,'COM-401 Cryptography and Security',0),(30,62,31,'COM-404 Information Theory and Coding',0),(31,62,32,'COM-417 Advanced Probability and Applications',0),(32,62,5,'CS-450 Advanced Algorithms',0),(33,62,9,'CS-550 Synthesis, Analysis and Verification',0),(34,62,4,'CS-445 Foundations of imaging science',0);
/*!40000 ALTER TABLE `forms_questions_choices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_questions_grades`
--

LOCK TABLES `forms_questions_grades` WRITE;
/*!40000 ALTER TABLE `forms_questions_grades` DISABLE KEYS */;
INSERT INTO `forms_questions_grades` VALUES (62,1.00,6.00,0.25,1),(75,1.00,6.00,0.25,3);
/*!40000 ALTER TABLE `forms_questions_grades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_questions_ranges`
--

LOCK TABLES `forms_questions_ranges` WRITE;
/*!40000 ALTER TABLE `forms_questions_ranges` DISABLE KEYS */;
/*!40000 ALTER TABLE `forms_questions_ranges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_reports`
--

LOCK TABLES `forms_reports` WRITE;
/*!40000 ALTER TABLE `forms_reports` DISABLE KEYS */;
INSERT INTO `forms_reports` VALUES (2,2,'Report EDIC'),(3,4,'Report EDIC');
/*!40000 ALTER TABLE `forms_reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_reports_fields`
--

LOCK TABLES `forms_reports_fields` WRITE;
/*!40000 ALTER TABLE `forms_reports_fields` DISABLE KEYS */;
INSERT INTO `forms_reports_fields` VALUES (1,1,'Director',2,'director',NULL),(2,1,'Co-Director',3,'codirector',NULL),(3,1,'S-Assessment',4,'question',18),(4,1,'D-Assessment',7,'question',27),(5,1,'S-Agree',5,'question',19),(6,1,'S-Look',6,'question',26),(7,1,'D-Look',8,'question',31),(8,1,'Progress',1,'progress',NULL),(9,2,'Director',2,'director',NULL),(10,2,'Co-Director',3,'codirector',NULL),(11,2,'Assessment',4,'question',44),(16,2,'Progress',1,'progress',NULL),(17,3,'Director',3,'director',NULL),(18,3,'Co-Director',4,'codirector',NULL),(19,3,'Assessment',6,'question',68),(20,3,'Progress',2,'progress',NULL),(21,3,'Mentor',5,'mentor',NULL),(22,3,'Date Enrolment',1,'date_enrolment',NULL);
/*!40000 ALTER TABLE `forms_reports_fields` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `forms_sections`
--

LOCK TABLES `forms_sections` WRITE;
/*!40000 ALTER TABLE `forms_sections` DISABLE KEYS */;
INSERT INTO `forms_sections` VALUES (4,2,'joint','Joint Review',1),(5,2,'director','Thesis Director\'s  Review',2),(6,3,'joint','Joint Review',1),(7,3,'director','Thesis Director\'s  Review',2),(8,4,'joint','Joint Review',1),(9,4,'director','Thesis Director\'s  Review',2);
/*!40000 ALTER TABLE `forms_sections` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
