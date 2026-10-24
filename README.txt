=====================================================================
  RESOLVEX - Complaint and Issue Tracking System  (Java, Eclipse)
=====================================================================

A web dashboard (HTML + CSS) made with plain Java.
No Tomcat, no jar, no database, no internet needed.

TWO dashboards, each with its own main file:
   AdminMain.java  ->  Admin dashboard   http://localhost:8080
   UserMain.java   ->  User dashboard    http://localhost:8081


---------------------------------------------------------------------
QUICK START FOR THE DEMO
---------------------------------------------------------------------
OPTION A - double click (needs JDK 17 or newer installed)
   Windows : RUN_DEMO.bat
   Linux/Mac: ./run_demo.sh
   Admin opens at http://localhost:8080 , User at http://localhost:8081

OPTION B - Eclipse
   File -> Import -> General -> Existing Projects into Workspace
   -> select the ResolveX folder -> Finish.
   Run  AdminMain.java  and  UserMain.java  (Run As -> Java Application).

The data is already inside the "data" folder, so the project works
immediately with all students and complaints.


---------------------------------------------------------------------
THE DATA  (nothing is random at run time, all numbers are consistent)
---------------------------------------------------------------------
   Section   Students   Usernames          Complaints
   CSE          50      cse1  ... cse50        50
   AIDS         40      aids1 ... aids40       40
   AIML         10      aiml1 ... aiml10       10
   Total       100                            100

* Every student has reported exactly one complaint, so every user sees
  a complaint as soon as he / she logs in.
* Every student can also support (vote for) complaints of his own
  section. Percentages are always out of the section strength
  (CSE 50, AIDS 40, AIML 10).
* Each section has its own dashboard. There is no mixed "whole college"
  complaint list.

LOGINS
   Admin    admin     admin123
   Students cse1 ... cse50, aids1 ... aids40, aiml1 ... aiml10
            (password of every student: pass123)

NOTE ABOUT NAMES: the full names of the students are placeholders
because the real class list was not available. Put the real names in
data/import/students.csv (column full_name) - see "USE YOUR OWN DATA".


---------------------------------------------------------------------
DATA FOLDER
---------------------------------------------------------------------
data/ResolveX_Data.xlsx   Excel workbook: Summary, Sections, Students,
                          Complaints (votes, support % and priority score
                          are Excel formulas), Formula (weights).
data/import/students.csv  the 100 students (username, full_name, section)
data/import/complaints.csv  the 100 complaints with status and votes
data/accounts.txt, complaints.txt, groups.txt, categories.txt
                          the live data used by the program. Admin and
                          User program share these files, so a change by
                          the admin is visible to the user on the next page.

USE YOUR OWN DATA
   1. Edit data/import/students.csv and complaints.csv in Excel
      (keep the heading row, save as CSV).
   2. Stop both programs, delete the 4 .txt files in the data folder.
   3. Run again. The program reads the CSV files and creates new .txt files.
   The section size is the number of students listed for the section.

RESET THE DEMO (waiting days start again, new complaints disappear)
   Stop both programs, then run RESET_DATA.bat (or ./reset_data.sh).


---------------------------------------------------------------------
ADMIN DASHBOARD - MENU
---------------------------------------------------------------------
   Sections Overview   one row per section: students, complaints, open,
                       in progress, resolved, safety critical, average support %.
   CSE / AIDS / AIML   the dashboard of that section only. The table shows
                       WHICH STUDENT reported each complaint (name and
                       username), votes, support %, score and status.
                       "Student-wise report" lists all students of the
                       section with the problem each one reported.
   Priority Ranking    open complaints of one section in rank order with
                       the reason (choose the section at the top).
   Statistics          counts by status and category for one section.
   Groups & Users      add a section, change strength, add students.
   Categories          add a category with keywords.
   Click a complaint title: set severity / safety critical, assign to a
   team, update status (a reason is required), change category.


---------------------------------------------------------------------
USER DASHBOARD - MENU
---------------------------------------------------------------------
   My Complaints       the complaint(s) reported by the logged in student.
   Section Complaints  all complaints of the student's own section with a
                       Support button (one vote per student).
   Report a Problem    new complaint for the own section. The category can be
                       detected from the words. If a similar open complaint of
                       the section exists, the student is asked to support it.
   On a complaint page the reporter can confirm the fix (Closed) or say the
   problem is not fixed (Reopened).
   A student never sees complaints of another section.


---------------------------------------------------------------------
THE PRIORITY FORMULA (PriorityCalculator.java)
---------------------------------------------------------------------
   Support %  = votes / students of the section x 100
   Score = 0.35 x Severity + 0.35 x Support % + 0.20 x Urgency
         + 0.10 x Waiting time          (each part is converted to 0-100)
   Severity = 1-5 x 20, Urgency = 1-5 x 20, Waiting = days x 100 / 30 (max 100)
The score is shown as a percentage (0% to 100%). 100% is possible only if
severity, support, urgency and waiting are all at the maximum.
Safety critical complaints are not given a fake 100: they keep their real
score and are simply listed first (red badge).

Example (complaint #1001, CSE): severity 3, urgency 4, 9 votes of 50
students (18%), waiting 22 days:
   0.35 x 60 + 0.35 x 18 + 0.20 x 80 + 0.10 x 73.33 = 50.63
Percentage is used instead of raw votes, so the small AIML section
(10 students) is not ignored because it has fewer students.


---------------------------------------------------------------------
PROJECT FILES
---------------------------------------------------------------------
web/style.css            colours and layout
com.resolvex             AdminMain, UserMain (the two programs)
com.resolvex.model       Complaint, Account, StudentGroup, Category
com.resolvex.service     ComplaintManager (logic), PriorityCalculator,
                         DataStore (text files + CSV import)
com.resolvex.web         AdminServer, UserServer, Layout, Session, Web
com.resolvex.util        PasswordUtil (passwords saved as SHA-256 hash)


---------------------------------------------------------------------
IF SOMETHING GOES WRONG
---------------------------------------------------------------------
- "Address already in use": the program is already running. Stop the old
  one (red square in the Eclipse Console, or close the black window).
- Page has no colours: run from the project folder (the web folder must be
  next to src). The scripts do this automatically.
- Red errors in Eclipse: Project -> Properties -> Java Build Path ->
  Libraries -> a JRE / JDK 17+ must be listed.
- Strange data: stop both programs and use the reset described above.
