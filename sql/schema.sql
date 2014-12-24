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
-- Table structure for table `forms_admins`
--

DROP TABLE IF EXISTS `forms_admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_admins` (
  `id_faculty` int(11) unsigned NOT NULL,
  `sciper` varchar(11) NOT NULL,
  PRIMARY KEY (`id_faculty`,`sciper`),
  KEY `sciper` (`sciper`),
  CONSTRAINT `forms_admins_ibfk_1` FOREIGN KEY (`id_faculty`) REFERENCES `forms_faculties` (`id`) ON DELETE CASCADE,
  CONSTRAINT `forms_admins_ibfk_2` FOREIGN KEY (`sciper`) REFERENCES `forms_users` (`sciper`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_answers`
--

DROP TABLE IF EXISTS `forms_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_answers` (
  `id_form_user` int(11) unsigned NOT NULL,
  `id_question` int(11) unsigned NOT NULL,
  `id_choice` int(11) unsigned DEFAULT NULL,
  `free_choice` text,
  `date_lastedit` datetime DEFAULT NULL,
  PRIMARY KEY (`id_form_user`,`id_question`),
  KEY `id_choice` (`id_choice`),
  KEY `id_question` (`id_question`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_definitions`
--

DROP TABLE IF EXISTS `forms_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_definitions` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_faculty` int(11) unsigned NOT NULL,
  `access_mode` enum('edee','edic') NOT NULL,
  `name` varchar(255) NOT NULL,
  `instructions_pre` text NOT NULL,
  `instructions_post` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_faculty` (`id_faculty`),
  CONSTRAINT `forms_definitions_ibfk_1` FOREIGN KEY (`id_faculty`) REFERENCES `forms_faculties` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_directors`
--

DROP TABLE IF EXISTS `forms_directors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_directors` (
  `id_faculty` int(11) unsigned NOT NULL,
  `sciper` varchar(11) NOT NULL,
  PRIMARY KEY (`id_faculty`,`sciper`),
  KEY `forms_directors_ibfk_2` (`sciper`),
  CONSTRAINT `forms_directors_ibfk_1` FOREIGN KEY (`id_faculty`) REFERENCES `forms_faculties` (`id`) ON DELETE CASCADE,
  CONSTRAINT `forms_directors_ibfk_2` FOREIGN KEY (`sciper`) REFERENCES `forms_users` (`sciper`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_directors_bck`
--

DROP TABLE IF EXISTS `forms_directors_bck`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_directors_bck` (
  `id_faculty` int(11) unsigned NOT NULL,
  `sciper` varchar(11) NOT NULL,
  PRIMARY KEY (`id_faculty`,`sciper`),
  KEY `forms_directors_ibfk_2` (`sciper`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_faculties`
--

DROP TABLE IF EXISTS `forms_faculties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_faculties` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `from_email` varchar(255) NOT NULL,
  `from_name` varchar(255) NOT NULL,
  `replyto_name` varchar(255) DEFAULT NULL,
  `replyto_email` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_filters`
--

DROP TABLE IF EXISTS `forms_filters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_filters` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_definition` int(11) unsigned NOT NULL,
  `id_question` int(11) unsigned DEFAULT NULL,
  `type` enum('question','director','codirector') NOT NULL DEFAULT 'question',
  `name` varchar(255) NOT NULL,
  `order` int(11) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_definition` (`id_definition`),
  KEY `id_question` (`id_question`),
  CONSTRAINT `forms_filters_ibfk_1` FOREIGN KEY (`id_definition`) REFERENCES `forms_definitions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `forms_filters_ibfk_2` FOREIGN KEY (`id_question`) REFERENCES `forms_questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_forms`
--

DROP TABLE IF EXISTS `forms_forms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_forms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_faculty` int(11) unsigned NOT NULL,
  `id_definition` int(11) unsigned NOT NULL,
  `year` year(4) NOT NULL,
  `midterm` tinyint(1) NOT NULL,
  `date_from` date NOT NULL,
  `date_to` date NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_form` (`id_definition`),
  KEY `id_faculty` (`id_faculty`),
  CONSTRAINT `forms_forms_ibfk_1` FOREIGN KEY (`id_definition`) REFERENCES `forms_definitions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `forms_forms_ibfk_2` FOREIGN KEY (`id_faculty`) REFERENCES `forms_faculties` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_forms_to_users`
--

DROP TABLE IF EXISTS `forms_forms_to_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_forms_to_users` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_form` int(11) unsigned NOT NULL,
  `sciper_user` varchar(11) NOT NULL,
  `date_lastedit` datetime DEFAULT NULL,
  `sciper_director` varchar(11) DEFAULT NULL,
  `sciper_codirector` varchar(11) DEFAULT NULL,
  `date_enrolment` date DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `keywords` text NOT NULL,
  `overview` text NOT NULL,
  `joint_complete` tinyint(1) NOT NULL,
  `student_complete` tinyint(1) NOT NULL,
  `director_complete` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_form` (`id_form`,`sciper_user`),
  KEY `sciper_user` (`sciper_user`),
  KEY `sciper_director` (`sciper_director`),
  KEY `sciper_codirector` (`sciper_codirector`),
  CONSTRAINT `forms_forms_to_users_ibfk_1` FOREIGN KEY (`id_form`) REFERENCES `forms_forms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `forms_forms_to_users_ibfk_2` FOREIGN KEY (`sciper_user`) REFERENCES `forms_users` (`sciper`) ON DELETE CASCADE,
  CONSTRAINT `forms_forms_to_users_ibfk_3` FOREIGN KEY (`sciper_director`) REFERENCES `forms_users` (`sciper`) ON DELETE CASCADE,
  CONSTRAINT `forms_forms_to_users_ibfk_4` FOREIGN KEY (`sciper_codirector`) REFERENCES `forms_users` (`sciper`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2776 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_notifications`
--

DROP TABLE IF EXISTS `forms_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_notifications` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_definition` int(11) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `subject` varchar(255) NOT NULL,
  `content` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_definition` (`id_definition`),
  CONSTRAINT `forms_notifications_ibfk_1` FOREIGN KEY (`id_definition`) REFERENCES `forms_definitions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_notifications_to_events`
--

DROP TABLE IF EXISTS `forms_notifications_to_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_notifications_to_events` (
  `id_notification` int(11) unsigned NOT NULL,
  `event` enum('joint_validated','joint_updated','joint_completed','student_completed','director_validated','director_updated','director_completed','form_completed') NOT NULL,
  `target` enum('director','codirector','student','faculty') NOT NULL,
  PRIMARY KEY (`id_notification`,`event`,`target`),
  CONSTRAINT `forms_notifications_to_events_ibfk_1` FOREIGN KEY (`id_notification`) REFERENCES `forms_notifications` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_parts`
--

DROP TABLE IF EXISTS `forms_parts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_parts` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_section` int(11) unsigned NOT NULL,
  `order` int(11) unsigned NOT NULL,
  `title` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_section` (`id_section`),
  CONSTRAINT `forms_parts_ibfk_1` FOREIGN KEY (`id_section`) REFERENCES `forms_sections` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_questions`
--

DROP TABLE IF EXISTS `forms_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_questions` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_definition` int(11) unsigned NOT NULL,
  `id_part` int(11) unsigned NOT NULL,
  `order` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `tooltip` text,
  `answer_type` enum('free','range','select','compactrange','compactrangetitle','coursesgrades','freegrades') NOT NULL,
  `optional` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_definition` (`id_definition`),
  KEY `id_part` (`id_part`),
  CONSTRAINT `forms_questions_ibfk_1` FOREIGN KEY (`id_definition`) REFERENCES `forms_definitions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `forms_questions_ibfk_2` FOREIGN KEY (`id_part`) REFERENCES `forms_parts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_questions_choices`
--

DROP TABLE IF EXISTS `forms_questions_choices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_questions_choices` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_question` int(11) unsigned NOT NULL,
  `order` int(11) unsigned NOT NULL,
  `value` varchar(255) NOT NULL,
  `freeifselected` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_question` (`id_question`),
  CONSTRAINT `forms_questions_choices_ibfk_1` FOREIGN KEY (`id_question`) REFERENCES `forms_questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_questions_grades`
--

DROP TABLE IF EXISTS `forms_questions_grades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_questions_grades` (
  `id_question` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `grade_from` decimal(4,2) NOT NULL,
  `grade_to` decimal(4,2) NOT NULL,
  `grade_step` decimal(4,2) NOT NULL,
  `cardinality` int(11) unsigned NOT NULL,
  PRIMARY KEY (`id_question`),
  CONSTRAINT `forms_questions_grades_ibfk_1` FOREIGN KEY (`id_question`) REFERENCES `forms_questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_questions_ranges`
--

DROP TABLE IF EXISTS `forms_questions_ranges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_questions_ranges` (
  `id_question` int(11) unsigned NOT NULL,
  `left` varchar(255) NOT NULL,
  `right` varchar(255) NOT NULL,
  `hasNA` tinyint(1) NOT NULL,
  PRIMARY KEY (`id_question`),
  CONSTRAINT `forms_questions_ranges_ibfk_1` FOREIGN KEY (`id_question`) REFERENCES `forms_questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_reports`
--

DROP TABLE IF EXISTS `forms_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_reports` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_definition` int(11) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_definition` (`id_definition`),
  CONSTRAINT `forms_reports_ibfk_1` FOREIGN KEY (`id_definition`) REFERENCES `forms_definitions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_reports_fields`
--

DROP TABLE IF EXISTS `forms_reports_fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_reports_fields` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_report` int(11) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `order` int(11) unsigned NOT NULL,
  `field` enum('question','progress','director','codirector','lab','title','keywords','overview','date_enrolment','mentor') NOT NULL,
  `id_question` int(11) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `id_report` (`id_report`),
  KEY `id_question` (`id_question`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_sections`
--

DROP TABLE IF EXISTS `forms_sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_sections` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_definition` int(11) unsigned NOT NULL,
  `section` enum('joint','student','director') NOT NULL,
  `title` varchar(255) NOT NULL,
  `order` int(11) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `id_definition` (`id_definition`),
  CONSTRAINT `forms_sections_ibfk_1` FOREIGN KEY (`id_definition`) REFERENCES `forms_definitions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_signatures`
--

DROP TABLE IF EXISTS `forms_signatures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_signatures` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_form_user` int(11) unsigned NOT NULL,
  `section` enum('joint','student','director') NOT NULL,
  `sciper` varchar(11) NOT NULL,
  `date` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sciper` (`sciper`),
  KEY `id_form_user` (`id_form_user`),
  CONSTRAINT `forms_signatures_ibfk_4` FOREIGN KEY (`id_form_user`) REFERENCES `forms_forms_to_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `forms_signatures_ibfk_5` FOREIGN KEY (`sciper`) REFERENCES `forms_users` (`sciper`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5605 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_signatures_backup`
--

DROP TABLE IF EXISTS `forms_signatures_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_signatures_backup` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_form_user` int(11) unsigned NOT NULL,
  `section` enum('joint','student','director') NOT NULL,
  `sciper` varchar(11) NOT NULL,
  `date` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sciper` (`sciper`),
  KEY `id_form_user` (`id_form_user`)
) ENGINE=InnoDB AUTO_INCREMENT=24025 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_users`
--

DROP TABLE IF EXISTS `forms_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_users` (
  `sciper` varchar(11) NOT NULL,
  `firstname` varchar(255) NOT NULL,
  `lastname` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  PRIMARY KEY (`sciper`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `forms_users_metadata`
--

DROP TABLE IF EXISTS `forms_users_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `forms_users_metadata` (
  `sciper` varchar(11) NOT NULL,
  `picture` varchar(255) DEFAULT NULL,
  `mentor` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`sciper`),
  CONSTRAINT `forms_users_metadata_ibfk_1` FOREIGN KEY (`sciper`) REFERENCES `forms_users` (`sciper`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
