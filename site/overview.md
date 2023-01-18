<p>
The Philter mod gives you a filter super-hopper that works with vanilla Minecraft or any mod.
Any kind of items can be diverted into a different
output container. Otherwise, it just passes them along like any other hopper.

<p>
Here's what it looks like in action:
<img src="visual.png" alt="a filter in place">

<p>
Overall the filter is a steel-blue hopper, and it has a redstone insert that does the
filtering. The blue
tab on the filter's top shows where normal items are directed. The redstone stripe flashes
when an item
is instead filtered to the hopper on the right.

<p>
You can set up the filter using its UI:
<img src="only_same_mode.png" alt="filter UI in 'Only Same' mode">

<p>
The area in the upper left is the basic hopper UI, and works just the same. On the right is
the filter
configuration. Here it is in "Only Same" mode, which means it will only put items into the
filter that
are already present in the target container. So if the target hopper has planks of every type,
only planks
will be diverted, and other item will continue onward.

<p>
The "Exact" button is off, so modifications will be ignored. If the target had iron
chestplate, this would allow
damaged or enchanted iron chestplates to also be put in the target. If "Exact" were on, only
chestplate that had
the same damage and enchantment would be diverted.

<p>
The other primary mode is "Match", where you specify tags and regular expressions to tell the
filter what to do:
<img src="matches_mode.png" alt="filter UI in 'Only Same' mode">

<p>
Here we've told the filter to match blocks that are covered by the tag "#wool" (all kinds of
wool) plus anything
that has "dirt" in the name. "Any of" can be switched to "All of", which would mean the item
would have to both
match "#wool" and have "dirt" in the name (which nothing does, so ...).

<p>
In "Match" mode, "Exact" means that NBT value will be included when string matching. So you
could match any item
that was enchanted with the pattern ".*Enchantments.*" because that string appears in the NBT
of enchanted items.

<p><a href="https://minecraft.fandom.com/wiki/Tag?so=search#Java_Edition_2">The list of standard
tags</a> will probably be helpful.
As for regular expressions, there are many tutorials on the web. Generally, text matches the
same text except for special symbols.
The most important are:
<ul>
<li><b><code>.</code></b> matches any single character, so "d.rt" matches both "dirt" and
"dart".
<li><b><code>*</code></b> means "zero or more repetitions", so ".*" means "zero or more of any
characters".
<li><b><code>^</code></b> matches the start of the text, so you could say "anything whose name
starts with blue" with the pattern "^blue.*", which would not match light blue things.
<li><b><code>$</code></b> matches the end of the text.
</ul>

The filter finds a match anywhere in the string unless you anchor it to the start and/or end of
the text. It also ignores
the case of the text, so "enchantments" would match both "enchantments" and "Enchantments".

<p>
Finally, the mode "None" just turns off the filtering entirely.

<p>
The third part of the UI, the part below your inventory, allows you to change the direction of
the filter. Like
a hopper, a filter can point in any direction but up, but also it cannot point in the same
direction as the regular
hopper output. When you place a filter, the filtered output is defined by how you're pointing,
but you can change
it afterwards using this part of the UI.

