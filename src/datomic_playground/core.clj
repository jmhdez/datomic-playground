(ns datomic-playground.core
  [:require [datomic.api :as d]])

(def uri "datomic:mem://koalite")

;; Crea una base de datos, borrando la que pudiera existir antes
(defn reset-db []
  (d/delete-database uri)
  (d/create-database uri))

;; Añado un atributo :hero/name. En la misma transacción se define el
;; atributo (que es una entidad más y se define en base a sus propios
;; atributos) y se "instala" en la base de datos, que en realidad es
;; asignarlo como valor dentro del atributo :db.install/attribute de
;; la entidad identificada por :db.part/db (creo que había un tipo
;; especial de atributo que servía como identificador de entidades y
;; se podía usar en lugar del id

(defn add-schema [conn]
  (d/transact
   conn
   [{:db/id #db/id[:db.part/db -1]
     :db/ident :hero/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc "El nombre del héroe"}
    [:db/add :db.part/db :db.install/attribute #db/id[:db.part/db -1]]]))

(defn add-heroes [conn]
  (d/transact
   conn
   [{:db/id #db/id[:db.part/user] :hero/name "Macareno"}
    {:db/id #db/id[:db.part/user] :hero/name "Luciano"}]))

(defn add-heroe [conn name]
  (d/transact
   conn
   [{:db/id #db/id[:db.part/user] :hero/name name}]))

(defn get-heroes [dbval]
  (let [heroes (d/q '[:find ?e :where [?e :hero/name]] dbval)]
    (map #(d/touch (d/entity dbval (first %))) heroes)))


(reset-db)

(def conn (d/connect uri))

(add-schema conn)

(add-heroe conn "Macareno")
(add-heroe conn "Luciano")

;; Añado un nuevo atributo al esquema
(d/transact
  conn
  [{:db/id #db/id[:db.part/db -1]
    :db/ident :hero/age
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "La edad"}
   [:db/add :db.part/db :db.install/attribute #db/id[:db.part/db -1]]])

;; Busco a Macareno para ponerle una edad

(def dbval (d/db conn))

(def macareno (ffirst (d/q '[:find ?e :where [?e :hero/name "Macareno"]] dbval)))

(def macareno-entity (d/entity dbval macareno))

;; Para ponerle edad, no debería necesitar ni siquiera el entity

(d/transact
 conn
 [{:db/id macareno
   :hero/age 1723}])

;; Ahora si consulto con el dbval anterior, no debería existir la edad
;; de macareno (esa es la idea de db as value, ¿no?)

(d/touch (d/entity dbval macareno)) ;; :db/id y :hero/name

;; Si consulto con la base de datos nueva debería aparecer
(d/touch (d/entity (d/db conn) macareno)) ;; efectivamente!!!


(defn add-person-schema [conn]
  (d/transact
   conn
   [{:db/id #db/id[:db.part/db -1]
     :db/ident :person/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc "Person's name"}
    {:db/id #db/id[:db.part/db -2]
     :db/ident :person/age
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Person's age"}
    {:db/id #db/id[:db.part/db -3]
     :db/ident :person/friends
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/many
     :db/doc "Person's friends (refs to another people)"}
    [:db/add :db.part/db :db.install/attribute #db/id[:db.part/db -1]]
    [:db/add :db.part/db :db.install/attribute #db/id[:db.part/db -2]]
    [:db/add :db.part/db :db.install/attribute #db/id[:db.part/db -3]]]))

(add-person-schema conn)

(d/transact
 conn
 [{:db/id #db/id[:db.part/user -1]
   :person/name "Alejandro"
   :person/age 53
   :person/friends [#db/id[:db.part/user -2]]}
  {:db/id #db/id[:db.part/user -2]
   :person/name "Lucas"
   :person/age 62
   :person/friends [#db/id[:db.part/user -1]]}])

