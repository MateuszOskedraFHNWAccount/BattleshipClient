java -jar target/battleship-server-1.0-SNAPSHOT.jar


OK Check network connectivity GET /ping expect {"ping": true}

define game key
define initial ship positions

get selected player which starts
(program will be function oriented)

middleware to handle sub-exponential backoffs, to accomodate long delays with no timeout

handle invalid moves

check if game is still ongoing

check if reqeust is 418 or 200 -> if 200 if there is a message.error -> display that


Routes
POST /game/join
    player : min. 3 char length
    gamekey: min. 3 char length
    ships
    

