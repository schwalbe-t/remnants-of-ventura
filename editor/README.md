### Remnants of Ventura World Editor

This is a development tool that allows editing of Remnants of Ventura worlds.

For example, it may be used to edit the main menu scene in `client/res/worlds/main_menu.json`.

It can be started using Gradle at the project root:
```
./gradlew :editor:run
```

Once started, the editor is controlled as follows:

#### All Modes
- `Ctrl`+`O` opens a world file (modifications are saved automatically)
- `T` toggles the tile grid visibility
- Scrolling controls camera zoom
- `W`, `A`, `S` and `D` move the camera
- `Escape` enables object selection mode (the default mode)
- `C` enables object creation mode and de-selects the currently selected object
- With an object selected:
  - `Tab` enables object property mode for the object
  - `1` enables object position mode for the object
  - `2` enables object rotation mode for the object

#### Object Selection Mode (`Escape`, default)
- Left-clicking an object anchor (white square) selects the object
- `Delete` or `Backspace` delete the currently selected object

#### Object Creation Mode (`C`)
- An object type may be selected on the left side
- Left-clicking places an object of the selected type in the world (hold `Shift` to align to grid)

#### Object Property Mode (`Tab`)
- Properties may be enabled, disabled or changed on the left side
- Summary of some properties:
  - `Type` is the same object type as selected in object creation mode
  - `Position` is the same position as modified by position mode
  - `Rotation` is the same rotation as modified by rotation mode, except that rotations around the X and Z axis are also available for some object types
  - `Scale` is the size factor
  - `TriggerFor` allows certain objects (such as buttons) to trigger other objects by name when they themselves are triggered
  - `Triggerable` defines the name that may be specified in the `TriggerFor` list of other objects to get triggered by them
  - `EnterWorldTrigger` makes the object act as a portal to another world when a player touches it (best used with object type `TRIGGER`)
  - `LeaveWorldTrigger` makes the object act as a portal to leave the current world when a player touchs it (best used with object type `TRIGGER`)
  - `CharacterAnimation` defines the person animation to be used for a `CHARACTER`
  - `CharacterDialogue` defines the dialogue to be used when talking to a `CHARACTER`
  - `CharacterStyleCustom` allows you to define the detailed style of a `CHARACTER`
  - `CharacterStylePreset` allows you to define the style of a `CHARACTER` using a pre-defined preset

#### Object Position Mode (`1`)
- Left-clicking moves the object where the cursor is (hold `Shift` to align to grid)

#### Object Rotation Mode (`2`)
- Left-dragging rotates the object around the Y axis (hold `Shift` to align to steps of 5 degrees)

#### Other Settings
The world file itself may also be modified using a text editor to change even more detailed settings, such as shading, spawn position, instance mode and more. 