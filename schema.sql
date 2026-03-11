CREATE DATABASE IF NOT EXISTS `testprogdb`;
USE `testprogdb`;

CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(64) NOT NULL,
  `is_admin` tinyint(1) NOT NULL DEFAULT '0',
  `salt` varchar(32) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `email` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
);

CREATE TABLE `workouts` (
  `id` varchar(36) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` int NOT NULL,
  `derived_from_plan_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_workouts_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_workouts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

CREATE TABLE `exercises` (
  `id` int NOT NULL AUTO_INCREMENT,
  `workout_id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `repetitions` int NOT NULL,
  `weight` double NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_workout` (`workout_id`),
  CONSTRAINT `fk_workout` FOREIGN KEY (`workout_id`) REFERENCES `workouts` (`id`) ON DELETE CASCADE
);

CREATE TABLE `planned_workouts` (
  `id` varchar(36) NOT NULL,
  `user_id` int NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `activate_time` timestamp NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `status` enum('PLANNED','MISSED','COMPLETED') NOT NULL DEFAULT 'PLANNED',
  `completed_workout_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_completed_workout` (`completed_workout_id`),
  KEY `idx_user_status` (`user_id`,`status`),
  CONSTRAINT `fk_completed_workout` FOREIGN KEY (`completed_workout_id`) REFERENCES `workouts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `planned_workouts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

CREATE TABLE `planned_exercises` (
  `id` int NOT NULL AUTO_INCREMENT,
  `planned_workout_id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `target_repetitions` int NOT NULL,
  `target_weight` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `planned_workout_id` (`planned_workout_id`),
  CONSTRAINT `planned_exercises_ibfk_1` FOREIGN KEY (`planned_workout_id`) REFERENCES `planned_workouts` (`id`) ON DELETE CASCADE
);