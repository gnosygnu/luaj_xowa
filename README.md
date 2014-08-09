luaj_xowa
=========

luaj_xowa is a fork of the 3.0 beta2 branch of https://sourceforge.net/projects/luaj/ . It is designed to be used with XOWA: an offline Wikipedia app.

Here are some examples of fixes / changes:

* Luaj does not handle UTF-16 surrogate character pairs. See https://en.wiktionary.org/wiki/𐇡
* Luaj omits some time / date formatters
* Luaj has some minor defects. For example, tonumber should trim \n. See: tonumber("1234\n")  

In addition, luaj_xowa has been modified to simultaneously support a Lua 5.1 and 5.2 environment. Note that luaj 3.0 is designed for 5.2 whereas Wikipedia uses 5.1
