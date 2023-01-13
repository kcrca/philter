Welcome!
-------

Welcome to McGUI! Relax! Have an ice cream!

McGUI helps wzyou build GUIs for Minecraft mods. It is expressly designed to
layer on top of the existing UI widgets. Most other GUI toolkits provide their
own widget and layout tools, which are definitely usually nicer, designed on
actual GUI toolkit principles. regularized, relatively complete.

The downsides are that they aren't skinable by resource pack makers, and they
don't adapt to changes to the the base Minecraft GUI tools. For example, if a
new accessibility feature is added to Minecraft, these kits will need to
re-implement it for you to get it.

McGUI instead asks the question: If one wre stuck with the Minecraft GUI system,
how much could we provide to make it more usable?

The original class Layout and its Placer inner class exemplify this. The native
system requires you to set the location and size of a widget when you create it,
and give no tools to relate widgets to each other, beyond some built in tools
for item stacks. With Layout and Placer, you can ask for coordinates relative to
another widget, place (say) an on/off button right next to a button, both of
which will adjust sizes based on the current interface language, whih changes
the size of the button.

While this doesn't make the code pretty, it makes it relatively simple to write
code that adjusts to the language, and lay out a set of related widgets.

Overview
--------

Let's look at some code using McGUI:

```
  @Override
  protected void init() {
    super.init();
    Layout layout = new Layout(this);
    layout.setPrefix("moodle");
    Placer p;
    Text button1Text = layout.text("button1");
    p = layout.placer().withText(button1Text).x(LEFT).y(ABOVE).inButton();
    var button1 = addDrawableChild(
        new ButtonWidget.Builder(button1Text, this::doStuff).dimensions(p.x(), p.y(), p.w(), p.h())
            .build());
    Text button2Text = layout.text("button2");
    String[] colors = new String[]{"red", "green", "blue"};
    p = layout.placer().withTexts(layout.texts(List.of(colors))).x(RIGHT, button1).y(MID, button1)
        .inButton();
    var colorButton = addDrawableChild(
        CyclingButtonWidget.builder(name -> layout.text((String) name))
            .tooltip(name -> layout.tooltip(name + ".tooltip"))
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, name) -> setColor(name)));
    p = layout.placer().lockButton().x(colorButton.getX()).y(BELOW, colorButton);
    addDrawableChild(new LockButtonWidget(p.x(), p.y(), this::toggleLock));
  }

```

After the `init()` setup, we create a Layout object passing in our Screen.

The first feature show sets the prefix used for text lookups. Later in the code
you'll see names like `"button1"` and `"red"`. This call means the real names
used for translate text will be `"moodle.button1"` and `"moodle.red"`. Most mods
use one prefix for all text keys, this shortens that code.

After declaring a reusable Placer variable, we then do the first text lookup.
This is equivalent to `new Text("moodle.button1")`. Let's pick this apart.

* `withText` tells the placer that we're placing something with this text. The
  placer set its width and height to be those of the text.
* `x` sets the x position to the left of the screen.
* `y` sets the y position to the top of the screen.
* `inButton`  says the text will be used inside a button. This adds margins for
  the button to the width and height to allow for bevels and to avoid cramping
  the text.

With that, the placer knows where we want to put something. And use that in adding
the first button child.
