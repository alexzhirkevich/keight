import kotlin.test.Test

/**
 * Tests for Unicode identifier support in JavaScript.
 * Verifies that property names containing various Unicode characters
 * (Chinese, Japanese, Korean, Russian, Arabic, Emoji, etc.) work correctly.
 */
class UnicodeIdentifierTest {

    // ========== Chinese Property Names ==========

    @Test
    fun chinesePropertyNames() = runtimeTest { runtime ->
        // Basic Chinese property access
        """
            var obj = { 名字: "张三", 年龄: 25 };
            obj.名字
        """.trimIndent().eval(runtime).assertEqualsTo("张三")

        """
            var obj = { 名字: "张三", 年龄: 25 };
            obj["名字"]
        """.trimIndent().eval(runtime).assertEqualsTo("张三")

        // Chinese property in nested object
        """
            var person = {
                用户信息: {
                    姓名: "李四",
                    地址: "北京"
                }
            };
            person.用户信息.姓名
        """.trimIndent().eval(runtime).assertEqualsTo("李四")

        // Chinese property with assignment
        """
            var obj = {};
            obj.计数器 = 0;
            obj.计数器++
            obj.计数器
        """.trimIndent().eval(runtime).assertEqualsTo(1L)

        // Multiple Chinese properties
        """
            var config = {
                启用: true,
                超时: 3000,
                端口: 8080
            };
            config.启用 && config.端口
        """.trimIndent().eval(runtime).assertEqualsTo(8080L)
    }

    @Test
    fun chinesePropertyWithBrackets() = runtimeTest { runtime ->
        """
            var map = {};
            map["中文键"] = "值";
            map["中文键"]
        """.trimIndent().eval(runtime).assertEqualsTo("值")

        // Computed property with Chinese
        """
            var key = "中文";
            var obj = { [key]: "computed" };
            obj[key]
        """.trimIndent().eval(runtime).assertEqualsTo("computed")
    }

    @Test
    fun chinesePropertyInFunctions() = runtimeTest { runtime ->
        """
            function getUser() {
                return { 名称: "测试", 积分: 100 };
            }
            getUser().名称
        """.trimIndent().eval(runtime).assertEqualsTo("测试")

        // Chinese parameter names
        """
            function 计算(数值, 系数) {
                return 数值 * 系数;
            }
            计算(5, 3)
        """.trimIndent().eval(runtime).assertEqualsTo(15L)
    }

    // ========== Japanese Property Names ==========

    @Test
    fun japanesePropertyNames() = runtimeTest { runtime ->
        """
            var user = { 名前: "田中", 年齢: 30 };
            user.名前
        """.trimIndent().eval(runtime).assertEqualsTo("田中")

        """
            var data = { 名前: "佐藤", 職業: "エンジニア" };
            data["職業"]
        """.trimIndent().eval(runtime).assertEqualsTo("エンジニア")

        // Japanese Hiragana and Katakana
        """
            var hiragana = { りんご: "apple" };
            hiragana.りんご
        """.trimIndent().eval(runtime).assertEqualsTo("apple")

        """
            var katakana = { コーヒー: "coffee" };
            katakana["コーヒー"]
        """.trimIndent().eval(runtime).assertEqualsTo("coffee")
    }

    @Test
    fun japaneseMixedKana() = runtimeTest { runtime ->
        """
            var mixed = { 
                コンピュータ: "computer",
                ネットワーク: "network",
                データ: "data"
            };
            mixed.コンピュータ
        """.trimIndent().eval(runtime).assertEqualsTo("computer")
    }

    // ========== Korean Property Names ==========

    @Test
    fun koreanPropertyNames() = runtimeTest { runtime ->
        """
            var person = { 이름: "김철수", 나이: 25 };
            person.이름
        """.trimIndent().eval(runtime).assertEqualsTo("김철수")

        """
            var product = { 제품명: "스마트폰", 가격: 1000000 };
            product["제품명"]
        """.trimIndent().eval(runtime).assertEqualsTo("스마트폰")
    }

    @Test
    fun koreanHangul() = runtimeTest { runtime ->
        """
            var data = {
                시작: "start",
                종료: "end",
                확인: "confirm"
            };
            data.시작
        """.trimIndent().eval(runtime).assertEqualsTo("start")
    }

    // ========== Russian Property Names ==========

    @Test
    fun russianPropertyNames() = runtimeTest { runtime ->
        """
            var user = { имя: "Иван", возраст: 30 };
            user.имя
        """.trimIndent().eval(runtime).assertEqualsTo("Иван")

        """
            var config = { включено: true, выключено: false };
            config["включено"]
        """.trimIndent().eval(runtime).assertEqualsTo(true)

        // Russian Cyrillic in nested objects
        """
            var server = {
                настройки: {
                    хост: "localhost",
                    порт: 8080
                }
            };
            server.настройки.хост
        """.trimIndent().eval(runtime).assertEqualsTo("localhost")
    }

    @Test
    fun russianMixed() = runtimeTest { runtime ->
        """
            var dict = {
                привет: "hello",
                мир: "world",
                язык: "language"
            };
            dict.привет + " " + dict.мир
        """.trimIndent().eval(runtime).assertEqualsTo("hello world")
    }

    // ========== Arabic Property Names ==========

    @Test
    fun arabicPropertyNames() = runtimeTest { runtime ->
        """
            var user = { اسم: "أحمد", عمر: 25 };
            user.اسم
        """.trimIndent().eval(runtime).assertEqualsTo("أحمد")

        """
            var settings = { تفعيل: true, إيقاف: false };
            settings["تفعيل"]
        """.trimIndent().eval(runtime).assertEqualsTo(true)
    }

    // ========== Greek Property Names ==========

    @Test
    fun greekPropertyNames() = runtimeTest { runtime ->
        """
            var data = { όνομα: "Σωκράτης", ηλικία: 30 };
            data.όνομα
        """.trimIndent().eval(runtime).assertEqualsTo("Σωκράτης")

        """
            var math = { π: 3.14159, ε: 0.0001 };
            math.π
        """.trimIndent().eval(runtime).assertEqualsTo(3.14159)
    }

    // ========== Hebrew Property Names ==========

    @Test
    fun hebrewPropertyNames() = runtimeTest { runtime ->
        """
            var user = { שם: "דוד", גיל: 30 };
            user.שם
        """.trimIndent().eval(runtime).assertEqualsTo("דוד")
    }

    // ========== Thai Property Names ==========

    @Test
    fun thaiPropertyNames() = runtimeTest { runtime ->
        """
            var product = { ชื่อ: "สินค้า", ราคา: 100 };
            product.ชื่อ
        """.trimIndent().eval(runtime).assertEqualsTo("สินค้า")
    }

    // ========== Vietnamese Property Names ==========

    @Test
    fun vietnamesePropertyNames() = runtimeTest { runtime ->
        """
            var user = { tên: "Nguyễn", tuổi: 25 };
            user.tên
        """.trimIndent().eval(runtime).assertEqualsTo("Nguyễn")

        """
            var text = { nội_dung: "Hello" };
            text.nội_dung
        """.trimIndent().eval(runtime).assertEqualsTo("Hello")
    }

    // ========== Emoji Property Names ==========

    @Test
    fun emojiPropertyNames() = runtimeTest { runtime ->
        """
            var icons = { "✅": "success", "❌": "error" };
            icons["✅"]
        """.trimIndent().eval(runtime).assertEqualsTo("success")

        // Emoji as property name (with quotes, since emoji identifiers may not be fully valid)
        """
            var emojis = {};
            emojis["🎉"] = "celebration";
            emojis["🎉"]
        """.trimIndent().eval(runtime).assertEqualsTo("celebration")
    }

    @Test
    fun emojiPropertyWithSpecialChars() = runtimeTest { runtime ->
        """
            var data = {};
            data["🔑"] = "key";
            data["📁"] = "folder";
            data["🔑"] + " and " + data["📁"]
        """.trimIndent().eval(runtime).assertEqualsTo("key and folder")
    }

    // ========== Unicode Escape Sequences ==========

    @Test
    fun unicodeEscapeInProperty() = runtimeTest { runtime ->
        // Test Unicode escape in object property name
        // Note: In Kotlin, "\\u" produces literal "\u" string
        """
            var obj = { "\u4e2d\u6587": "Chinese" };
            obj["\u4e2d\u6587"]
        """.trimIndent().eval(runtime).assertEqualsTo("Chinese")

        // Test ASCII Unicode escape
        """
            var obj = {};
            obj["\u0041\u0042\u0043"] = "ABC";
            obj["\u0041\u0042\u0043"]
        """.trimIndent().eval(runtime).assertEqualsTo("ABC")
    }

    // ========== Mixed ASCII and Unicode ==========

    @Test
    fun mixedAsciiAndUnicode() = runtimeTest { runtime ->
        """
            var user = {
                name: "John",
                姓名: "张三",
                age: 30,
                年龄: 25
            };
            user.name + " / " + user.姓名
        """.trimIndent().eval(runtime).assertEqualsTo("John / 张三")

        """
            var mixed = {
                userName: "admin",
                用户名: "管理员",
                password: "pass123",
                密码: "密码123"
            };
            mixed.userName + ", " + mixed.用户名
        """.trimIndent().eval(runtime).assertEqualsTo("admin, 管理员")
    }

    // ========== Object Methods with Unicode ==========

    @Test
    fun objectMethodsWithUnicode() = runtimeTest { runtime ->
        """
            var obj = {
                获取数据: function() { return 42; },
                处理: function(x) { return x * 2; }
            };
            obj.获取数据()
        """.trimIndent().eval(runtime).assertEqualsTo(42L)

        """
            var obj = {
                获取数据: function() { return 42; },
                处理: function(x) { return x * 2; }
            };
            obj.处理(21)
        """.trimIndent().eval(runtime).assertEqualsTo(42L)
    }

    @Test
    fun arrowFunctionsWithUnicode() = runtimeTest { runtime ->
        """
            var calc = {
                加法: (a, b) => a + b,
                乘法: (a, b) => a * b
            };
            calc.加法(3, 5)
        """.trimIndent().eval(runtime).assertEqualsTo(8L)

        """
            var calc = {
                加法: (a, b) => a + b,
                乘法: (a, b) => a * b
            };
            calc.乘法(4, 6)
        """.trimIndent().eval(runtime).assertEqualsTo(24L)
    }

    // ========== Class Properties with Unicode ==========

    @Test
    fun classPropertiesWithUnicode() = runtimeTest { runtime ->
        """
            class User {
                constructor(名称, 年龄) {
                    this.名称 = 名称;
                    this.年龄 = 年龄;
                }
                介绍() {
                    return this.名称 + " - " + this.年龄;
                }
            }
            var user = new User("张三", 25);
            user.介绍()
        """.trimIndent().eval(runtime).assertEqualsTo("张三 - 25")
    }

    @Test
    fun classGetterSetterWithUnicode() = runtimeTest { runtime ->
        """
            class Person {
                constructor() {
                    this._name = "";
                }
                get 姓名() {
                    return this._name;
                }
                set 姓名(value) {
                    this._name = value;
                }
            }
            var p = new Person();
            p.姓名 = "李四";
            p.姓名
        """.trimIndent().eval(runtime).assertEqualsTo("李四")
    }

    // ========== Destructuring with Unicode ==========

    @Test
    fun destructuringWithUnicode() = runtimeTest { runtime ->
        """
            var { 名称, 年龄 } = { 名称: "王五", 年龄: 35 };
            名称 + " - " + 年龄
        """.trimIndent().eval(runtime).assertEqualsTo("王五 - 35")
    }

    @Test
    fun arrayDestructuringWithUnicode() = runtimeTest { runtime ->
        """
            var [第一, 第二] = ["first", "second"];
            第一 + " and " + 第二
        """.trimIndent().eval(runtime).assertEqualsTo("first and second")
    }

    // ========== for...in / for...of with Unicode ==========

    @Test
    fun forInWithUnicode() = runtimeTest { runtime ->
        """
            var obj = { 名称: "test", 年龄: 20 };
            var keys = [];
            for (var key in obj) {
                keys.push(key);
            }
            keys
        """.trimIndent().eval(runtime).assertEqualsTo(listOf("名称", "年龄"))
    }

    // ========== Object Spread/Rest with Unicode ==========

    @Test
    fun objectSpreadWithUnicode() = runtimeTest { runtime ->
        """
            var obj1 = { 名称: "first", 值: 1 };
            var obj2 = { ...obj1, 新值: 2 };
            obj2.名称
        """.trimIndent().eval(runtime).assertEqualsTo("first")
    }

    // ========== Optional Chaining with Unicode ==========

    @Test
    fun optionalChainingWithUnicode() = runtimeTest { runtime ->
        """
            var obj = {
                用户: {
                    地址: {
                        城市: "北京"
                    }
                }
            };
            obj?.用户?.地址?.城市
        """.trimIndent().eval(runtime).assertEqualsTo("北京")

        // When accessing through null, optional chaining returns undefined
        """
            var obj = {
                用户: null
            };
            typeof (obj?.用户?.地址?.城市)
        """.trimIndent().eval(runtime).assertEqualsTo("undefined")
    }

    // ========== Delete with Unicode ==========

    @Test
    fun deleteWithUnicode() = runtimeTest { runtime ->
        """
            var obj = { 名称: "test", 年龄: 20 };
            delete obj.名称;
            Object.keys(obj)
        """.trimIndent().eval(runtime).assertEqualsTo(listOf("年龄"))
    }

    // ========== 'in' Operator with Unicode ==========

    @Test
    fun inOperatorWithUnicode() = runtimeTest { runtime ->
        """
            var obj = { 名称: "test", 年龄: 20 };
            "名称" in obj
        """.trimIndent().eval(runtime).assertEqualsTo(true)

        """
            var obj = { 名称: "test", 年龄: 20 };
            "不存在" in obj
        """.trimIndent().eval(runtime).assertEqualsTo(false)
    }

    // ========== Computed Property Names ==========

    @Test
    fun computedPropertyWithUnicode() = runtimeTest { runtime ->
        """
            var prefix = "用户";
            var obj = {
                [prefix + "名"]: "张三",
                [prefix + "ID"]: 12345
            };
            obj.用户名 + " - " + obj.用户ID
        """.trimIndent().eval(runtime).assertEqualsTo("张三 - 12345")
    }

    // ========== Long Unicode Strings ==========

    @Test
    fun longChinesePropertyNames() = runtimeTest { runtime ->
        """
            var obj = { 
                这是一个非常长的中文字符串属性名: "值1",
                另一个很长的属性名用于测试: "值2"
            };
            obj.这是一个非常长的中文字符串属性名
        """.trimIndent().eval(runtime).assertEqualsTo("值1")
    }

    // ========== JSON-like Data with Unicode ==========

    @Test
    fun jsonDataWithUnicode() = runtimeTest { runtime ->
        """
            var jsonData = {
                "响应": {
                    "状态码": 200,
                    "消息": "成功",
                    "数据": {
                        "用户列表": [
                            {"名称": "张三", "邮箱": "zhang@example.com"},
                            {"名称": "李四", "邮箱": "li@example.com"}
                        ]
                    }
                }
            };
            jsonData.响应.数据.用户列表[0].名称
        """.trimIndent().eval(runtime).assertEqualsTo("张三")
    }

    // ========== Template Literal with Unicode ==========

    @Test
    fun templateLiteralWithUnicode() = runtimeTest { runtime ->
        // Use Kotlin escape for ${} to prevent Kotlin string interpolation
        """
            var 名称 = "世界";
            `你好, ${'$'}{名称}!`
        """.trimIndent().eval(runtime).assertEqualsTo("你好, 世界!")
    }

    // ========== Edge Cases ==========

    @Test
    fun unicodeIdentifierEdgeCase() = runtimeTest { runtime ->
        // Property starting with number-like Unicode (mathematical bold digits)
        """
            var obj = {};
            obj["𝟙"] = "one";
            obj["𝟚"] = "two";
            obj["𝟛"] = "three";
            obj["𝟙"]
        """.trimIndent().eval(runtime).assertEqualsTo("one")
    }

    @Test
    fun mixedScriptPropertyNames() = runtimeTest { runtime ->
        """
            var i18n = {
                hello: "Hello",
                你好: "Chinese",
                привет: "Russian",
                bonjour: "French",
                こんにちは: "Japanese"
            };
            i18n.hello + ", " + i18n.你好 + ", " + i18n.привет
        """.trimIndent().eval(runtime).assertEqualsTo("Hello, Chinese, Russian")
    }

    // ========== Variable Declaration with Unicode ==========

    @Test
    fun variableDeclarationWithUnicode() = runtimeTest { runtime ->
        """
            var 计数器 = 0;
            计数器++;
            计数器++;
            计数器
        """.trimIndent().eval(runtime).assertEqualsTo(2L)

        """
            let 最大值 = 100;
            let 最小值 = 1;
            最大值 - 最小值
        """.trimIndent().eval(runtime).assertEqualsTo(99L)

        """
            const 常量π = 3.14159;
            常量π
        """.trimIndent().eval(runtime).assertEqualsTo(3.14159)
    }

    @Test
    fun functionWithUnicodeName() = runtimeTest { runtime ->
        """
            function 获取数据() {
                return { 状态: "success", 数据: [1, 2, 3] };
            }
            获取数据().状态
        """.trimIndent().eval(runtime).assertEqualsTo("success")
    }

    // ========== Property Shorthand ==========

    @Test
    fun propertyShorthandWithUnicode() = runtimeTest { runtime ->
        """
            var 名称 = "test";
            var obj = { 名称 };
            obj.名称
        """.trimIndent().eval(runtime).assertEqualsTo("test")
    }
}
