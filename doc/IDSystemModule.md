# ID System Module

Module that is a Management for IDs. It can create IDs and save them. 
The ID System is not a Thread.

## Variables

| Name                        | Type                  | Standard Value               | Description                        | Authors Note               |
| --------------------------- | --------------------- | ---------------------------- | ---------------------------------- | -------------------------- |
| TAG                         | String (static final) | "IDSystem"                   | Tag for the Logger                 | more on that in [Logger]() |
| idCharacterAlphabet         | String (static final) | "abcdefghijklmnopqrstuvwxyz" | Characters used in the IDs         |                            |
| idNumbersAlphabet           | String (static final) | "1234567890"                 | Numbers used in the IDs            |                            |
| idSpecialCharactersAlphabet | String (static final) | "_-"                         | special Characters used in the IDs |                            |
| idSize                      | int (static final)    | 8                            | allowed size of an ID              |                            |
| idSaveFile                  | File (static)         | null                         | File where the IDs will be saved   |                            |

---

## Constructor

No Constructor, since everything is static

---

## Public Methods

### Interface Methods :

| Name                   | Parameter list    | return Value | Description                                                  | Authors Note |
| ---------------------- | ----------------- | ------------ | ------------------------------------------------------------ | ------------ |
| (static) setIDSaveFile | idSaveFile (File) | void         | setting the ID Save File to read from or write to            |              |
| (static) createNewID   | empty             | String       | creates a new ID , saves it in the idSaveFile and returns it |              |
| (static) writeEntry    | id (String)       | boolean      | appending a new id to the save file, if it doesn't already exist |              |

### Getter and Setter

| Name               | Parameter List | return Value | Description                               | Authors Note |
| ------------------ | -------------- | ------------ | ----------------------------------------- | ------------ |
| (static) getAllIDs | empty          | String       | returns all saved ids from the idSaveFile |              |

---

## Private Methods

| Name                     | Parameter List                                 | return Value | Description                                                  | Authors Note |
| ------------------------ | ---------------------------------------------- | ------------ | ------------------------------------------------------------ | ------------ |
| (static) idAlreadyExists | id (String)                                    | boolean      | checks the file if the ID already exists                     |              |
| (static) generateID      | rng (Random), characters(String), length (int) | String       | Generates a new ID with the given Randomizer, allowed Characters and length |              |
| (static) getAllIDs       | empty                                          | String       | returns all saved ids from the idSaveFile                    |              |

