-- ////////////////////////////////////////////////////////////////////////////
-- // Update OBM MySQL Database from 1.0 to 1.1                              //
-- ////////////////////////////////////////////////////////////////////////////
-- // $Id$
-- ////////////////////////////////////////////////////////////////////////////


-------------------------------------------------------------------------------
-- Global Information table
-------------------------------------------------------------------------------
UPDATE ObmInfo set obminfo_value='1.1' where obminfo_name='db_version';

-------------------------------------------------------------------------------
-- Update Contact table
-------------------------------------------------------------------------------
-- Add columns 
ALTER TABLE Contact ADD COLUMN contact_comment2 text NULL after contact_comment;
ALTER TABLE Contact ADD COLUMN contact_comment3 text NULL after contact_comment2;
ALTER TABLE Contact ADD COLUMN contact_date timestamp(14) after contact_privacy;
ALTER TABLE Contact ADD COLUMN contact_category5_id int(8) DEFAULT 0 after contact_comment3;


-------------------------------------------------------------------------------
-- Tables needed for Contact module
-------------------------------------------------------------------------------

--
-- Table structure for table 'ContactCategory3'
--
CREATE TABLE ContactCategory3 (
  contactcategory3_id          int(8) auto_increment,
  contactcategory3_timeupdate  timestamp(14),
  contactcategory3_timecreate  timestamp(14),
  contactcategory3_userupdate  int(8) default 0,
  contactcategory3_usercreate  int(8) default 0,
  contactcategory3_code        varchar(10) default '',
  contactcategory3_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (contactcategory3_id)
);

--
-- Table structure for table 'ContactCategory3Link'
--
CREATE TABLE ContactCategory3Link (
  contactcategory3link_category_id  int(8) NOT NULL default 0,
  contactcategory3link_contact_id   int(8) NOT NULL default 0,
  PRIMARY KEY (contactcategory3link_category_id,contactcategory3link_contact_id),
  INDEX contcat3_idx_cont (contactcategory3link_contact_id)
);

--
-- Table structure for table 'ContactCategory4'
--
CREATE TABLE ContactCategory4 (
  contactcategory4_id          int(8) auto_increment,
  contactcategory4_timeupdate  timestamp(14),
  contactcategory4_timecreate  timestamp(14),
  contactcategory4_userupdate  int(8) default 0,
  contactcategory4_usercreate  int(8) default 0,
  contactcategory4_code        varchar(10) default '',
  contactcategory4_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (contactcategory4_id)
);


--
-- Table structure for table 'ContactCategory4Link'
--
CREATE TABLE ContactCategory4Link (
  contactcategory4link_category_id  int(8) NOT NULL default 0,
  contactcategory4link_contact_id   int(8) NOT NULL default 0,
  PRIMARY KEY (contactcategory4link_category_id,contactcategory4link_contact_id),
  INDEX contcat4_idx_cont (contactcategory4link_contact_id)
);

--
-- Table structure for table 'ContactCategory5'
--
CREATE TABLE ContactCategory5 (
  contactcategory5_id          int(8) auto_increment,
  contactcategory5_timeupdate  timestamp(14),
  contactcategory5_timecreate  timestamp(14),
  contactcategory5_userupdate  int(8) default 0,
  contactcategory5_usercreate  int(8) default 0,
  contactcategory5_code        varchar(10) default '',
  contactcategory5_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (contactcategory5_id)
);

-------------------------------------------------------------------------------
-- Create table structure for table 'CompanyCategory1'
-------------------------------------------------------------------------------
-- Create table 'CompanyCategory1'
CREATE TABLE CompanyCategory1 (
  companycategory1_id          int(8) auto_increment,
  companycategory1_timeupdate  timestamp(14),
  companycategory1_timecreate  timestamp(14),
  companycategory1_userupdate  int(8) NOT NULL default 0,
  companycategory1_usercreate  int(8) NOT NULL default 0,
  companycategory1_code        varchar(10) NOT NULL default '',
  companycategory1_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (companycategory1_id)
);

-- Update table 'CompanyCategory1'
INSERT INTO CompanyCategory1 (
  companycategory1_id,
  companycategory1_timeupdate,
  companycategory1_timecreate,
  companycategory1_userupdate,
  companycategory1_usercreate,
  companycategory1_code,
  companycategory1_label
)
SELECT
  companycategory_id,
  companycategory_timeupdate,
  companycategory_timecreate,
  companycategory_userupdate,
  companycategory_usercreate,
  companycategory_code,
  companycategory_label
FROM 
  CompanyCategory;

-- DROP table CompanyCategory
DROP TABLE IF EXISTS CompanyCategory;


-------------------------------------------------------------------------------
-- Create table structure for table 'CompanyCategory1Link'
-------------------------------------------------------------------------------
-- Create table 'CompanyCategory1Link'
CREATE TABLE CompanyCategory1Link (
  companycategory1link_category_id  int(8) NOT NULL default 0,
  companycategory1link_company_id   int(8) NOT NULL default 0,
  PRIMARY KEY (companycategory1link_category_id,companycategory1link_company_id),
  INDEX compcat1_idx_comp (companycategory1link_company_id)
);

-- Update table 'CompanyCategory1'
INSERT INTO CompanyCategory1Link (
  companycategory1link_category_id,
  companycategory1link_company_id
)
SELECT
  companycategorylink_category_id,
  companycategorylink_company_id
FROM
  CompanyCategoryLink;

-- DROP table CompanyCategory1Link
DROP TABLE IF EXISTS CompanyCategoryLink;


-------------------------------------------------------------------------------
-- Create table structure for table 'DealCategory1'
-------------------------------------------------------------------------------
-- Table structure for table 'DealCategory1'
CREATE TABLE DealCategory1 (
  dealcategory1_id          int(8) auto_increment,
  dealcategory1_timeupdate  timestamp(14),
  dealcategory1_timecreate  timestamp(14),
  dealcategory1_userupdate  int(8) default 0,
  dealcategory1_usercreate  int(8) default 0,
  dealcategory1_code        varchar(10) default '',
  dealcategory1_label       varchar(100) NOT NULL default '',
  PRIMARY KEY (dealcategory1_id)
);

-- Update table 'DealCategory1'
INSERT INTO DealCategory1 (
  dealcategory1_id,
  dealcategory1_timeupdate,
  dealcategory1_timecreate,
  dealcategory1_userupdate,
  dealcategory1_usercreate,
  dealcategory1_code,
  dealcategory1_label
)
SELECT
  dealcategory_id,
  dealcategory_timeupdate,
  dealcategory_timecreate,
  dealcategory_userupdate,
  dealcategory_usercreate,
  dealcategory_code,
  dealcategory_label
FROM
  DealCategory;

-- DROP table DealCategory
DROP TABLE IF EXISTS DealCategory;

-------------------------------------------------------------------------------
-- Table structure for table 'DealCategory1Link'
-------------------------------------------------------------------------------
-- Create table structure for table 'DealCategory1Link'
CREATE TABLE DealCategory1Link (
  dealcategory1link_category_id  int(8) NOT NULL default 0,
  dealcategory1link_deal_id      int(8) NOT NULL default 0,
  PRIMARY KEY (dealcategory1link_category_id,dealcategory1link_deal_id)
);

-- Update table 'DealCategory1Link'
INSERT INTO DealCategory1Link (
  dealcategory1link_category_id,
  dealcategory1link_deal_id
)
SELECT
  dealcategorylink_category_id,
  dealcategorylink_deal_id
FROM
  DealCategoryLink;

-- DROP table DealCategory1Link
DROP TABLE IF EXISTS DealCategory1Link;

-------------------------------------------------------------------------------
-- Table structure for table 'CalendarCategory1'
-------------------------------------------------------------------------------
CREATE TABLE CalendarCategory1 (
  calendarcategory1_id          int(8) auto_increment,
  calendarcategory1_timeupdate  timestamp(14),
  calendarcategory1_timecreate  timestamp(14),
  calendarcategory1_userupdate  int(8) default NULL,
  calendarcategory1_usercreate  int(8) default NULL,
  calendarcategory1_code        varchar(10) default '',
  calendarcategory1_label       varchar(128) default NULL,
  PRIMARY KEY (calendarcategory1_id)
);

-- Update table 'CalendarCategory1'
INSERT INTO CalendarCategory1 (
  calendarcategory1_id,
  calendarcategory1_timeupdate,
  calendarcategory1_timecreate,
  calendarcategory1_userupdate,
  calendarcategory1_usercreate,
  calendarcategory1_label
)
SELECT
  calendarcategory_id,
  calendarcategory_timeupdate,
  calendarcategory_timecreate,
  calendarcategory_userupdate,
  calendarcategory_usercreate,
  calendarcategory_label
FROM
  CalendarCategory;

-- DROP table CalendarCategory 
DROP TABLE IF EXISTS CalendarCategory;

-------------------------------------------------------------------------------
-- Update CalendarEvent table
-------------------------------------------------------------------------------
-- correctness : _category_id -> _category1_id
ALTER TABLE CalendarEvent CHANGE calendarevent_category_id calendarevent_category1_id int(8) default NULL;

-------------------------------------------------------------------------------
-- Update Incident table
-------------------------------------------------------------------------------
-- correctness : _cat1_id -> _category1_id
ALTER TABLE Incident CHANGE incident_cat1_id incident_category1_id int(8) default NULL;

-------------------------------------------------------------------------------
-- Update DocumentCategory1 table
-------------------------------------------------------------------------------
ALTER TABLE DocumentCategory1 ADD COLUMN documentcategory1_code varchar(10) default '' after documentcategory1_usercreate;

-------------------------------------------------------------------------------
-- Update DocumentCategory2 table
-------------------------------------------------------------------------------
ALTER TABLE DocumentCategory2 ADD COLUMN documentcategory2_code varchar(10) default '' after documentcategory2_usercreate;

