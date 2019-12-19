(ns adventure.core
  (:gen-class))

(def maze {
    1 [2 3 4] 2 [1 5 6] 3 [1 7 8] 4 [1 9 10] 5 [2 9 11]
    6 [2 7 12] 7 [3 6 13] 8 [3 10 14] 9 [4 5 15] 10 [4 8 16]
    11 [5 12 17] 12 [6 11 18] 13 [7 14 18] 14 [8 13 19]
    15 [9 16 17] 16 [10 15 19] 17 [11 0 15] 18 [12 13 0]
    19 [14 16 0] 0 [17 18 19]})
(def maze-size (count maze))

(defn vector-has [v elt]
  (some #{elt} v))

(defn rand-unique
    "Pick a random number from 0 to `max-1` that is not in the set `exclude`.  Does not check for errors."
    [max exclude]
    (let [pick (rand-int max)]
        (if (exclude pick) (rand-unique max exclude) pick)))

(defn new-game []
    (let [wumpus (rand-int maze-size)
          bat1 (rand-unique maze-size #{wumpus})
          bat2 (rand-unique maze-size #{wumpus bat1})
          pit1 (rand-unique maze-size #{wumpus bat1 bat2})
          pit2 (rand-unique maze-size #{wumpus bat1 bat2 pit1})
          adv (rand-unique maze-size #{wumpus bat1 bat2 pit1 pit2})
          ]
        {:wumpus wumpus
         :bat1 bat1 :bat2 bat2
         :pit1 pit1 :pit2 pit2
         :adventurer adv
         :status :alive}))

(defn move-wumpus [state]
    (println "The wumpus moves!")
    (let [new-loc ((-> :wumpus state maze) (rand-int 3))]
        (if-not (== new-loc (:adventurer state))
            (println "You are lucky; the wumpus didn't find you."))
        (assoc state :wumpus new-loc)))

(defn validate-path
     "Given a location `loc` and a path, verify that the path exists in the maze.
     If a room is invalid, pick a random one."
     ([loc path] (validate-path loc path []))
     ([loc path accum]
       (cond (empty? path) accum
             (vector-has (maze loc) (first path)) (validate-path (first path) (rest path) (conj accum (first path)))
             :else (let [randloc ((maze loc) (rand-int 3))]
                     (validate-path randloc (rest path) (conj accum randloc))))))

(defn shoot
     "Shoot the crooked arrow up to five rooms, the path is contained in vector cave.  If a room
     is not reachable, pick a random one instead."
     [state path]
     (cond (> (count path) 5)
           (do (println "There are too many rooms in your path.  Nothing happens.")
               state)

           (empty? path) (do (println "You miss.")
                             (move-wumpus state))
           (== (first path) (state :wumpus))
           (do (println "You got the wumpus! Congratulations!")
               (assoc state :status :win))
           (== (first path) (state :adventurer))
           (do (println "Ouch!  You shot yourself with the arrow!")
               (assoc state :status :dead))
           :else (shoot state (rest path))))


(defn bat-attack
     "The bat picks up the adventure and drops them into a random cave.  The bat then moves to another
     random cave.  We assume the bat was the one in the same room as the adventurer."
     [state]
     (let [the-bat (if (== (state :bat1) (state :adventurer)) :bat1 :bat2)
           new-location (rand-unique maze-size #{(state the-bat)}) ; anywhere not here
           new-bat (rand-unique maze-size #{(state the-bat) new-location}) ; anywhere else
           ]
         (do (println "The bat picks you up and drops you into a random room!")
             (merge state {the-bat new-bat :adventurer new-location}))))

(defn what-happens [state]
     (let [loc (state :adventurer)]
         (cond (== loc (:wumpus state))
               (do (println "On no!  The wumpus got you!  Munch munch munch!!")
                   {assoc state :status :dead})
               (contains? (conj #{} (state :pit1) (state :pit2)) loc)
               (do (println "On no!  You fell down a pit!  AAAAaaaaaaaahhhh....!  Splat!!")
                   (assoc state :status :dead))
               (contains? (conj #{} (state :bat1) (state :bat2)) loc)
               (do (println "Oh no!  There's a bat in here!")
                   (bat-attack state))
               :else state)))

(defn what-nearby [state]
     (let [loc (state :adventurer)]
         (if (some #{(state :pit1) (state :pit2)} (maze loc))
             (println "You feel a draft."))
         (if (some #{(state :bat1) (state :bat2)} (maze loc))
             (println "You hear a bat."))
         (if (some #{(state :wumpus)} (maze loc))
             (println "You smell a wumpus!"))
         state))

(defn repl [state]
     (loop [state state]
         (if (= (:status state) :alive)
             (do
                (println "You are in room" (state :adventurer))
                (println "The rooms that are connected are" (-> :adventurer state maze))
                (what-nearby state)
                (println "What do you want to do? ([M]ove/[S]hoot/[Q]uit) ")
         (let [choice (read-line)]
             (cond (= choice "M")
                   (do (println "Which room?")
                       (let [room (read-string (read-line))]
                           (if (vector-has (-> :adventurer state maze) room)
                               (recur (what-happens (assoc state :adventurer room)))
                               (do (println "You can't go there.")
                                   (recur state)))))
                   (= choice "Q")  (println "Thanks for playing!")
                   (= choice "S")
                   (do (println "Enter a vector of up to five rooms.")
                       (recur (what-happens (shoot state (validate-path (state :adventurer) (read-string (read-line))))))))))
             (println "Game over."))))




(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (repl (new-game)))