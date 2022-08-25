/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

CREATE TABLE IF NOT EXISTS `recycle_bin` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `site_id`           BIGINT          NOT NULL,
    `manifest_path`     VARCHAR(96)     NOT NULL,
    `timestamp`         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `user_id`           BIGINT          NOT NULL,
    `comment`           VARCHAR(1024)   NOT NULL,
    `is_published`      VARCHAR(16)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE uq_recycle_bin_site_manifest_path (`site_id`, `manifest_path`),
    FOREIGN KEY recycle_bin_id_user_id (`user_id`) REFERENCES `user` (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8
    ROW_FORMAT = DYNAMIC ;

CREATE TABLE IF NOT EXISTS `recycle_bin_items`
(
   `recycle_bin_id`     BIGINT          NOT NULL,
   `item_id`            BIGINT          NOT NULL,
   `item_label`         VARCHAR(256)    NOT NULL,
   PRIMARY KEY (`recycle_bin_id`, `item_id`),
   FOREIGN KEY recycle_bin_items_recycle_bin_id (`recycle_bin_id`) REFERENCES `recycle_bin` (`id`)
        ON DELETE CASCADE,
   FOREIGN KEY recycle_bin_items_item_id (`item_id`) REFERENCES `item` (`id`)
        ON DELETE RESTRICT
)
    ENGINE = InnoDB
    DEFAULT CHARSET = utf8
    ROW_FORMAT = DYNAMIC ;

UPDATE `_meta` SET `version` = '4.0.1.9' ;
