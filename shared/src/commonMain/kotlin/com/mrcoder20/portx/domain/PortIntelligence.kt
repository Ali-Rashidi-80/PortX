package com.mrcoder20.portx.domain

data class PortSecurityDossier(
    val port: Int,
    val rfcStandard: String,
    val categoryKey: String,
    val tierKey: String,
    val encryptionKey: String,
    val attackVectorsEn: String,
    val attackVectorsFa: String,
    val attackVectorsRu: String,
    val cveReferences: List<String>,
    val hardeningGuideEn: String,
    val hardeningGuideFa: String,
    val hardeningGuideRu: String
) {
    fun getAttackVectors(lang: String): String = when (lang) {
        "fa" -> attackVectorsFa
        "ru" -> attackVectorsRu
        else -> attackVectorsEn
    }

    fun getHardeningGuide(lang: String): String = when (lang) {
        "fa" -> hardeningGuideFa
        "ru" -> hardeningGuideRu
        else -> hardeningGuideEn
    }
}

object PortIntelligence {
    fun getDossier(port: Int, rawService: String? = null): PortSecurityDossier {
        val tier = when (port) {
            in 0..1023 -> "privileged_port"
            in 1024..49151 -> "registered_port"
            else -> "dynamic_port"
        }

        return when (port) {
            20, 21 -> PortSecurityDossier(
                port = port,
                rfcStandard = "RFC 959 (File Transfer Protocol)",
                categoryKey = "cat_files",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Cleartext credential sniffing, Anonymous login abuse, Brute-force attacks, FTP Bounce.",
                attackVectorsFa = "شنود اطلاعات هویتی به دلیل ارسال متن ساده، سوءاستفاده از دسترسی ناشناس (Anonymous) و حملات جستجوی فراگیر (Brute-force).",
                attackVectorsRu = "Перехват учетных данных в открытом виде, анонимный вход, брутфорс, атаки типа FTP Bounce.",
                cveReferences = listOf("CVE-2015-3306 (ProFTPD)", "CVE-2011-2523 (vsftpd Backdoor)", "CVE-2020-9273"),
                hardeningGuideEn = "Migrate immediately to SFTP (SSH Port 22) or FTPS with enforced TLS 1.3. Disable anonymous authentication.",
                hardeningGuideFa = "سرویس را به SFTP یا FTPS ارتقا دهید و احراز هویت ناشناس را غیرفعال کنید.",
                hardeningGuideRu = "Перейдите на SFTP (порт 22) или FTPS с обязательным TLS 1.3. Отключите анонимный доступ."
            )
            22 -> PortSecurityDossier(
                port = 22,
                rfcStandard = "RFC 4253 (Secure Shell - SSHv2)",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_ssh",
                attackVectorsEn = "Automated credential brute-forcing, Stolen private keys, Outdated OpenSSH RCE vulnerabilities.",
                attackVectorsFa = "حملات خودکار حدس گذرواژه (Brute-force)، سرقت کلیدهای خصوصی SSH و آسیب‌پذیری‌های ارتقای سطح دسترسی در نسخه‌های قدیمی.",
                attackVectorsRu = "Брутфорс паролей, утечка закрытых SSH-ключей, RCE-уязвимости устаревших версий OpenSSH.",
                cveReferences = listOf("CVE-2024-6387 (regreSSHion)", "CVE-2023-38408 (PKCS#11)", "CVE-2016-0777"),
                hardeningGuideEn = "Disable PasswordAuthentication and Root Login in sshd_config. Enforce Ed25519 public key auth, Fail2Ban, and IP whitelisting.",
                hardeningGuideFa = "ورود با رمزعبور و دسترسی مستقیم root را غیرفعال کنید. احراز هویت با کلید عمومی و مسدودسازی خودکار آی‌پی (Fail2Ban) را فعال نمایید.",
                hardeningGuideRu = "Отключите вход по паролю и вход root. Используйте ключи Ed25519, настройте Fail2Ban и ограничьте доступ по IP."
            )
            23 -> PortSecurityDossier(
                port = 23,
                rfcStandard = "RFC 854 (Telnet Protocol)",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Unencrypted terminal traffic, Network credential interception, Man-in-the-Middle (MITM).",
                attackVectorsFa = "ارسال متن ساده تمامی دستورات و رمزها در بستر شبکه، رهگیری ترافیک توسط مهاجمین میانی (MITM).",
                attackVectorsRu = "Перехват всех команд и паролей в открытом виде, атаки Man-in-the-Middle (MITM).",
                cveReferences = listOf("CVE-2020-10188", "Legacy Cleartext Protocol"),
                hardeningGuideEn = "Disable Telnet daemon unconditionally. Replace with encrypted SSH (Port 22) or Out-of-Band management.",
                hardeningGuideFa = "سرویس Telnet را کاملاً مسدود و غیرفعال کرده و پروتکل امن SSH را جایگزین کنید.",
                hardeningGuideRu = "Полностью отключите Telnet. Замените на зашифрованный SSH (порт 22)."
            )
            25 -> PortSecurityDossier(
                port = 25,
                rfcStandard = "RFC 5321 (Simple Mail Transfer Protocol)",
                categoryKey = "cat_mail",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Open Mail Relay, Spam distribution, User enumeration (VRFY/EXPN), STARTTLS stripping.",
                attackVectorsFa = "سوءاستفاده به عنوان رله باز ایمیل (Open Relay)، ارسال اسپم و فیشینگ، شناسایی نام کاربران.",
                attackVectorsRu = "Использование открытого релея (Open Relay), рассылка спама, перечисление пользователей (VRFY).",
                cveReferences = listOf("CVE-2019-15846 (Exim)", "CVE-2020-28018", "Open Relay"),
                hardeningGuideEn = "Disable open relaying. Enforce strict SPF, DKIM, DMARC validation and mandatory STARTTLS.",
                hardeningGuideFa = "قابلیت Open Relay را غیرفعال کرده و رکوردهای امنیتی SPF، DKIM و DMARC را پیکربندی کنید.",
                hardeningGuideRu = "Отключите Open Relay. Настройте строгую проверку SPF, DKIM, DMARC и обязательный STARTTLS."
            )
            53 -> PortSecurityDossier(
                port = 53,
                rfcStandard = "RFC 1035 (Domain Name System)",
                categoryKey = "cat_infra",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "DNS Amplification DDoS, Cache poisoning (Kaminsky attack), Unauthorized AXFR Zone Transfers, DNS Tunneling.",
                attackVectorsFa = "حملات منع سرویس تقویت‌شده (DDoS Amplification)، مسموم‌سازی کش DNS، انتقال غیرمجاز زون دامنه (AXFR) و تونل‌زنی داده.",
                attackVectorsRu = "DNS Amplification DDoS, отравление кэша (атака Каминского), несанкционированный перенос зоны (AXFR).",
                cveReferences = listOf("CVE-2021-25216 (BIND9)", "CVE-2020-1350 (SIGRed)", "CVE-2008-1447"),
                hardeningGuideEn = "Disable open recursion for external queries. Restrict AXFR zone transfers to authorized secondary nameservers. Deploy DNSSEC.",
                hardeningGuideFa = "انتقال زون (AXFR) را محدود به سرورهای مجاز کنید، بازگشت باز (Recursion) برای عموم را ببندید و DNSSEC را فعال نمایید.",
                hardeningGuideRu = "Отключите открытую рекурсию для внешних запросов. Ограничьте AXFR. Внедрите DNSSEC."
            )
            80, 8080, 8000, 8008, 8888, 9000 -> PortSecurityDossier(
                port = port,
                rfcStandard = "RFC 9110 (Hypertext Transfer Protocol)",
                categoryKey = "cat_web",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Cleartext HTTP traffic sniffing, Web vulnerabilities (SQLi, XSS, SSRF, RCE), Session hijacking.",
                attackVectorsFa = "شنود ترافیک بدون رمزنگاری، آسیب‌پذیری‌های وب نظیر تزریق کد، SQLi، XSS و جعل هویت نشست کاربری.",
                attackVectorsRu = "Перехват открытого трафика, веб-уязвимости (SQLi, XSS, SSRF, RCE), перехват сессий.",
                cveReferences = listOf("CVE-2021-41773 (Apache)", "CVE-2021-42013", "OWASP Top 10"),
                hardeningGuideEn = "Redirect all HTTP traffic to HTTPS (Port 443) with HSTS headers. Enforce TLS 1.3 and deploy Web Application Firewall (WAF).",
                hardeningGuideFa = "تمامی درخواست‌ها را با هدر HSTS به HTTPS هدایت کنید و از فایروال وب (WAF) استفاده نمایید.",
                hardeningGuideRu = "Настройте автоматический редирект на HTTPS (порт 443) с HSTS. Внедрите WAF."
            )
            443, 8443, 9443 -> PortSecurityDossier(
                port = port,
                rfcStandard = "RFC 8446 (HTTP over TLS 1.3)",
                categoryKey = "cat_web",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Web application exploits, TLS certificate forgery, Expired certificates, Insecure cipher suites.",
                attackVectorsFa = "حملات لایه ۷ وب‌اپلیکیشن، پیکربندی ضعیف الگوریتم‌های رمزنگاری، اکسپلویت‌های وب‌سرور.",
                attackVectorsRu = "Атаки на веб-приложения (L7), устаревшие наборы шифров SSL/TLS, уязвимости веб-серверов.",
                cveReferences = listOf("CVE-2014-0160 (Heartbleed)", "CVE-2022-22965 (Spring4Shell)", "CVE-2021-44228 (Log4Shell)"),
                hardeningGuideEn = "Disable legacy TLS 1.0/1.1 and weak ciphers. Maintain valid automated SSL/TLS certificates and enforce CSP & HSTS.",
                hardeningGuideFa = "پروتکل‌های منسوخ TLS 1.0 و 1.1 را غیرفعال کنید. گواهینامه‌های SSL معتبر تهیه کرده و هدرهای امنیتی را تنظیم نمایید.",
                hardeningGuideRu = "Отключите устаревшие TLS 1.0/1.1 и слабые шифры. Используйте актуальные сертификаты и HSTS."
            )
            135 -> PortSecurityDossier(
                port = 135,
                rfcStandard = "MS-RPC / DCE 1.1 Endpoint Mapper",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_binary",
                attackVectorsEn = "DCOM / RPC Remote Code Execution, RPC endpoint enumeration, Lateral movement, Potato privilege escalation.",
                attackVectorsFa = "اجرای کد از راه دور (DCOM/RPC)، شناسایی اندپوینت‌های سیستمی ویندوز، نفوذ جانبی در اکتیو دایرکتوری و ارتقای سطح دسترسی.",
                attackVectorsRu = "RCE через DCOM/RPC, перечисление эндпоинтов RPC, горизонтальное перемещение в домене (Lateral Movement).",
                cveReferences = listOf("CVE-2022-26809 (RPC RCE)", "CVE-2003-0352 (MSBlaster)", "JuicyPotato"),
                hardeningGuideEn = "Block Port 135 on perimeter firewalls. Restrict RPC communication strictly to trusted internal subnets.",
                hardeningGuideFa = "پورت ۱۳۵ را در فایروال لبه به طور کامل مسدود کنید و دسترسی را فقط به زیرشبکه مدیریت داخلی محدود سازید.",
                hardeningGuideRu = "Заблокируйте порт 135 на внешнем брандмауэре. Ограничьте доступ доверенными подсетями."
            )
            139, 445 -> PortSecurityDossier(
                port = port,
                rfcStandard = "MS-SMB / RFC 1001 (Server Message Block)",
                categoryKey = "cat_files",
                tierKey = tier,
                encryptionKey = "enc_kerberos",
                attackVectorsEn = "EternalBlue SMBv1 RCE, SMB Relay attacks, Null Session enumeration, Pass-the-Hash / Pass-the-Ticket, Ransomware propagation.",
                attackVectorsFa = "حملات EternalBlue در SMBv1، رله کردن احراز هویت NTLM، ورود با حساب کاربری خالی (Null Session) و انتشار باج‌افزار در شبکه.",
                attackVectorsRu = "EternalBlue SMBv1 RCE, атаки SMB Relay, Pass-the-Hash, распространение программ-вымогателей.",
                cveReferences = listOf("CVE-2017-0144 (EternalBlue)", "CVE-2020-0796 (SMBGhost)", "CVE-2020-1472 (Zerologon)"),
                hardeningGuideEn = "Disable SMBv1 entirely. Require SMB Signing and SMB Encryption (SMBv3). Block Inbound Ports 139/445 on WAN boundaries.",
                hardeningGuideFa = "پروتکل منسوخ SMBv1 را غیرفعال کرده و امضای دیجیتال و رمزنگاری SMBv3 را اجباری کنید. این پورت را روی اینترنت مسدود نمایید.",
                hardeningGuideRu = "Полностью отключите SMBv1. Включите обязательное подписание SMB и шифрование SMBv3. Закройте порт наружу."
            )
            502 -> PortSecurityDossier(
                port = 502,
                rfcStandard = "Modbus-IDA / IEC 61158 (Modbus/TCP)",
                categoryKey = "cat_industrial",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Unauthenticated PLC command injection, Coil/Register overwrite, Denial of Service on physical machinery, ICS MITM.",
                attackVectorsFa = "تزریق دستورات بدون احراز هویت به PLCها، بازنویسی رجیسترهای سنسورها و عملگرها، توقف اضطراری و دستکاری تجهیزات صنعتی.",
                attackVectorsRu = "Внедрение команд PLC без аутентификации, перезапись регистров, срыв технологических процессов, атаки на АСУ ТП.",
                cveReferences = listOf("ICS-CERT Advisories", "Cleartext SCADA Protocol", "Modbus Function 0x05/0x06 Injection"),
                hardeningGuideEn = "Isolate Modbus/TCP inside air-gapped or OT Industrial DMZ networks. Deploy ICS Deep Packet Inspection (DPI) firewalls. Never expose to Public IP.",
                hardeningGuideFa = "تجهیزات Modbus را در شبکه تفکیک‌شده صنعتی (OT DMZ) ایزوله کرده و از فایروال‌های پایش عمیق بسته (DPI) استفاده کنید. هرگز این پورت را در اینترنت عمومی قرار ندهید.",
                hardeningGuideRu = "Изолируйте сеть OT/АСУ ТП в выделенной демилитаризованной зоне. Используйте межсетевые экраны с DPI. Никогда не открывайте наружу."
            )
            1433 -> PortSecurityDossier(
                port = 1433,
                rfcStandard = "MS-TDS (Microsoft SQL Server)",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "SA account brute-forcing, xp_cmdshell command execution, SQL injection lateral movement.",
                attackVectorsFa = "حملات جستجوی فراگیر روی کاربر sa، اجرای دستورات سیستم‌عامل از طریق xp_cmdshell و استخراج داده‌های حساس.",
                attackVectorsRu = "Брутфорс учетной записи 'sa', выполнение команд через xp_cmdshell, эксфильтрация баз данных.",
                cveReferences = listOf("CVE-2020-0618", "CVE-2019-1068", "xp_cmdshell abuse"),
                hardeningGuideEn = "Disable default 'sa' account. Enforce Windows Authentication only, TLS encryption, and restrict listening to localhost/app servers.",
                hardeningGuideFa = "حساب پیش‌فرض sa را غیرفعال کرده و احراز هویت ویندوزی، رمزنگاری TLS و محدودسازی آی‌پی را اعمال کنید.",
                hardeningGuideRu = "Отключите учетную запись 'sa'. Включите только Windows-аутентификацию и обязательное шифрование."
            )
            3306 -> PortSecurityDossier(
                port = 3306,
                rfcStandard = "MySQL Client/Server Protocol",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Root password brute forcing, UDF (User Defined Function) privilege escalation, Data exfiltration.",
                attackVectorsFa = "حملات Brute-force به کاربر root، ارتقای سطح دسترسی به سیستم‌عامل از طریق UDF و سرقت پایگاه داده.",
                attackVectorsRu = "Брутфорс паролей root, эскалация привилегий через UDF, несанкционированная выгрузка баз данных.",
                cveReferences = listOf("CVE-2016-6662", "CVE-2021-27928", "UDF Dynamic Library Exploits"),
                hardeningGuideEn = "Bind MySQL strictly to 127.0.0.1 or internal private network. Disable remote root login and enforce SSL/TLS connections.",
                hardeningGuideFa = "موتور پایگاه‌داده را به 127.0.0.1 متصل کنید. دسترسی مستقیم root از راه دور را غیرفعال کرده و SSL را اجباری نمایید.",
                hardeningGuideRu = "Привяжите MySQL к 127.0.0.1 или внутренней сети. Запретите удаленный вход root, включите SSL."
            )
            5432 -> PortSecurityDossier(
                port = 5432,
                rfcStandard = "PostgreSQL Client Connection Protocol",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Postgres user brute-forcing, COPY FROM PROGRAM command execution, Trust authentication bypass.",
                attackVectorsFa = "حملات جستجوی گذرواژه، اجرای دستورات شل با استفاده از قابلیت COPY PROGRAM و سوءاستفاده از تنظیمات pg_hba.conf.",
                attackVectorsRu = "Брутфорс учетных записей, выполнение команд через COPY FROM PROGRAM, ошибки в pg_hba.conf.",
                cveReferences = listOf("CVE-2019-9193", "CVE-2018-1058", "pg_hba trust bypass"),
                hardeningGuideEn = "Enforce SCRAM-SHA-256 authentication in pg_hba.conf. Reject cleartext/trust access and restrict listening interface.",
                hardeningGuideFa = "احراز هویت SCRAM-SHA-256 را در فایل pg_hba.conf اجباری کنید و اتصالات را تنها به شبکه خصوصی محدود سازید.",
                hardeningGuideRu = "Используйте аутентификацию SCRAM-SHA-256 в pg_hba.conf. Запретите режим 'trust' и используйте SSL."
            )
            6379 -> PortSecurityDossier(
                port = 6379,
                rfcStandard = "RESP (REdis Serialization Protocol)",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Unauthenticated Redis RCE via cron overwrite, SSH authorized_keys injection, Data wiping.",
                attackVectorsFa = "اجرای کد از راه دور به دلیل عدم احراز هویت در ردیس، تزریق کلید عمومی به authorized_keys لینوکس و پاک‌سازی داده‌ها.",
                attackVectorsRu = "RCE без аутентификации через запись в cron, внедрение SSH-ключей в authorized_keys, удаление данных.",
                cveReferences = listOf("CVE-2022-0543", "Unauthenticated Redis RCE", "Redis Eval Lua Sandbox Escape"),
                hardeningGuideEn = "Enable requirepass with strong password. Rename or disable DANGEROUS commands (FLUSHALL, CONFIG, EVAL). Bind to 127.0.0.1.",
                hardeningGuideFa = "قابلیت requirepass با رمزعبور قوی را فعال کرده و دستورات خطرناک CONFIG و FLUSHALL را تغییر نام دهید یا غیرفعال کنید.",
                hardeningGuideRu = "Включите requirepass с надежным паролем. Переименуйте или отключите команды CONFIG и FLUSHALL. Привяжите к 127.0.0.1."
            )
            27017 -> PortSecurityDossier(
                port = 27017,
                rfcStandard = "MongoDB Wire Protocol",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Unauthenticated NoSQL database dumping, Ransomware database wiping, Injection attacks.",
                attackVectorsFa = "دسترسی مستقیم بدون احراز هویت به اسناد دیتابیس، باج‌افزارهای پاک‌کننده دیتابیس و افشای گسترده داده‌ها.",
                attackVectorsRu = "Несанкционированный доступ без аутентификации, стирание баз данных вымогателями, NoSQL-инъекции.",
                cveReferences = listOf("CVE-2019-2386", "Unauthenticated Mongo Ransom Attacks"),
                hardeningGuideEn = "Enable auth = true in mongod.conf. Bind to localhost, enforce TLS and role-based access control (RBAC).",
                hardeningGuideFa = "گزینه auth = true را در mongod.conf فعال کرده، دسترسی را به localhost محدود و RBAC را پیاده‌سازی نمایید.",
                hardeningGuideRu = "Включите auth = true в mongod.conf. Привяжите к localhost, настройте TLS и ролевой доступ (RBAC)."
            )
            110, 995 -> PortSecurityDossier(
                port = port,
                rfcStandard = if (port == 995) "RFC 8314 (POP3 over TLS)" else "RFC 1939 (Post Office Protocol v3)",
                categoryKey = "cat_mail",
                tierKey = tier,
                encryptionKey = if (port == 995) "enc_tls" else "enc_cleartext",
                attackVectorsEn = "Cleartext credential capture, Mailbox exfiltration, Password spray attacks.",
                attackVectorsFa = "شنود اطلاعات هویتی به دلیل ارسال رمز بدون رمزنگاری در پورت ۱۱۰، استخراج ایمیل‌ها و حملات حدس گذرواژه.",
                attackVectorsRu = "Перехват учетных данных в открытом виде (порт 110), несанкционированная выгрузка почты.",
                cveReferences = listOf("Cleartext Mail Authentication", "CVE-2020-12872"),
                hardeningGuideEn = "Deprecate plaintext Port 110. Enforce TLS 1.3 encrypted POP3S on Port 995 or migrate to modern API/IMAPS.",
                hardeningGuideFa = "پورت ۱۱۰ را غیرفعال کرده و اتصال رمزنگاری‌شده POP3S روی پورت ۹۹۵ با TLS 1.3 را اجباری کنید.",
                hardeningGuideRu = "Отключите порт 110. Включите зашифрованный POP3S на порту 995 с TLS 1.3."
            )
            143, 993 -> PortSecurityDossier(
                port = port,
                rfcStandard = if (port == 993) "RFC 9051 / RFC 8314 (IMAP over TLS)" else "RFC 3501 (Internet Message Access Protocol)",
                categoryKey = "cat_mail",
                tierKey = tier,
                encryptionKey = if (port == 993) "enc_tls" else "enc_cleartext",
                attackVectorsEn = "Credential theft in transit, IMAP injection, Mail folder exfiltration.",
                attackVectorsFa = "سرقت گذرواژه در مسیر ارسال، تزریق دستورات IMAP و دسترسی غیرمجاز به تمامی پوشه‌های ایمیل کاربر.",
                attackVectorsRu = "Перехват паролей в транзите, IMAP-инъекции, несанкционированный доступ к почтовым папкам.",
                cveReferences = listOf("CVE-2020-12872", "CVE-2018-19518 (UW-IMAP)"),
                hardeningGuideEn = "Enforce IMAPS Port 993 with valid SSL certificates. Disable unencrypted Port 143 and legacy TLS ciphers.",
                hardeningGuideFa = "پورت ۱۴۳ متن‌ساده را بسته و از IMAPS روی پورت ۹۹۳ همراه با احراز هویت دوعاملی استفاده نمایید.",
                hardeningGuideRu = "Используйте IMAPS на порту 993. Закройте порт 143 и отключите устаревшие шифры."
            )
            161, 162 -> PortSecurityDossier(
                port = port,
                rfcStandard = "RFC 1157 / RFC 3416 (Simple Network Management Protocol)",
                categoryKey = "cat_infra",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Default 'public'/'private' community string abuse, Full network device configuration extraction, SNMP Amplification DDoS.",
                attackVectorsFa = "سوءاستفاده از عبارت امنیتی پیش‌فرض (public/private)، استخراج توپولوژی و تنظیمات حساس روترها و حملات منع سرویس بازتابی.",
                attackVectorsRu = "Использование стандартных community strings (public/private), выгрузка конфигураций устройств, атаки SNMP Amplification.",
                cveReferences = listOf("CVE-2017-6736 (Cisco SNMP RCE)", "Default Community String Vulnerabilities"),
                hardeningGuideEn = "Upgrade immediately to SNMPv3 with authPriv (SHA-256 + AES encryption). Disable SNMPv1/v2c and restrict by ACL.",
                hardeningGuideFa = "سرویس را به SNMPv3 با رمزنگاری SHA/AES ارتقا دهید. نسخه‌های ۱ و ۲ را غیرفعال کرده و دسترسی را با ACL محدود کنید.",
                hardeningGuideRu = "Перейдите на SNMPv3 с обязательным шифрованием authPriv (SHA/AES). Отключите SNMPv1/v2c."
            )
            389, 636 -> PortSecurityDossier(
                port = port,
                rfcStandard = if (port == 636) "RFC 4513 (LDAP over TLS - LDAPS)" else "RFC 4511 (Lightweight Directory Access Protocol)",
                categoryKey = "cat_infra",
                tierKey = tier,
                encryptionKey = if (port == 636) "enc_tls" else "enc_cleartext",
                attackVectorsEn = "Active Directory domain enumeration, LDAP Injection, Cleartext credential sniffing (Port 389), Kerberoasting reconnaissance.",
                attackVectorsFa = "شناسایی ساختار اکتیو دایرکتوری و کاربران شبکه، تزریق کد به کوئری‌های LDAP، شنود اعتبارنامه‌ها و مقدمه‌چینی حملات Kerberos.",
                attackVectorsRu = "Перечисление объектов Active Directory, LDAP-инъекции, перехват учетных данных на порту 389, Kerberoasting.",
                cveReferences = listOf("CVE-2021-42287", "CVE-2021-42278", "Active Directory ZeroLogon"),
                hardeningGuideEn = "Enforce LDAP Channel Binding and LDAP Signing (LDAPS Port 636). Block cleartext Port 389 across perimeter.",
                hardeningGuideFa = "امضای دیجیتال LDAP و اتصال امن LDAPS روی پورت ۶۳۶ را اجباری کرده و پورت ۳۸۹ را روی مرز شبکه مسدود کنید.",
                hardeningGuideRu = "Включите обязательную подпись LDAP и LDAPS (порт 636). Заблокируйте порт 389 на периметре."
            )
            1521 -> PortSecurityDossier(
                port = 1521,
                rfcStandard = "Oracle TNS (Transparent Network Substrate)",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "TNS Poisoning / MITM, SID brute-forcing, Default DBA credential exploitation, PL/SQL injection.",
                attackVectorsFa = "مسموم‌سازی TNS، حملات جستجوی شناسه‌های SID پایگاه‌داده، سوءاستفاده از کاربران پیش‌فرض SYS/SYSTEM و تزریق کدهای PL/SQL.",
                attackVectorsRu = "TNS Poisoning, брутфорс SID, эксплуатация стандартных учетных записей DBA, PL/SQL-инъекции.",
                cveReferences = listOf("CVE-2012-1675 (TNS Poisoning)", "CVE-2021-2182", "Oracle Critical Patch Updates"),
                hardeningGuideEn = "Set VALID_NODE_CHECKING_REGISTRATION_LISTENER = ON. Protect TNS with Native Network Encryption (NNE) or TLS.",
                hardeningGuideFa = "قابلیت بررسی نودهای معتبر (VNCR) را در فایل listener.ora فعال کرده و رمزنگاری NNE یا TLS را پیاده‌سازی نمایید.",
                hardeningGuideRu = "Включите VNCR в listener.ora. Защитите TNS с помощью Native Network Encryption (NNE) или TLS."
            )
            5900, 5901 -> PortSecurityDossier(
                port = port,
                rfcStandard = "RFC 6143 (Remote Framebuffer - VNC)",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Unauthenticated desktop view/control, VNC DES password cracking (8-char limit), Keystroke injection.",
                attackVectorsFa = "مشاهده و کنترل مستقیم دسکتاپ بدون احراز هویت، شکستن گذرواژه‌های کوتاه الگوریتم DES در VNC و تزریق فرامین کیبورد.",
                attackVectorsRu = "Неаутентифицированный просмотр/управление рабочим столом, подбор 8-значных паролей DES VNC.",
                cveReferences = listOf("CVE-2019-15691 (TightVNC)", "CVE-2019-15692", "UltraVNC Authentication Bypass"),
                hardeningGuideEn = "Never expose VNC directly to WAN. Tunnel VNC strictly through encrypted SSH (Port 22) or corporate VPN.",
                hardeningGuideFa = "هرگز پورت VNC را مستقیماً در اینترنت باز نگذارید؛ دسترسی را منحصراً از طریق تونل رمزنگاری‌شده SSH یا VPN برقرار کنید.",
                hardeningGuideRu = "Никогда не открывайте VNC наружу. Пробрасывайте VNC исключительно через зашифрованный SSH-туннель или VPN."
            )
            9200, 9300 -> PortSecurityDossier(
                port = port,
                rfcStandard = "Elasticsearch REST API / Cluster Discovery",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Unauthenticated index exfiltration, Database deletion/ransom, Log4Shell RCE in older versions.",
                attackVectorsFa = "تخلیه کامل ایندکس‌ها و اسناد بدون احراز هویت، باج‌افزارهای پاک‌کننده دیتابیس الاستیک و آسیب‌پذیری Log4Shell در نسخه‌های قدیمی.",
                attackVectorsRu = "Выгрузка индексов без аутентификации, удаление данных вымогателями, Log4Shell в старых версиях.",
                cveReferences = listOf("CVE-2021-44228 (Log4Shell)", "CVE-2015-1427", "Unauthenticated Elasticsearch Ransom"),
                hardeningGuideEn = "Enable xpack.security.enabled = true. Configure HTTPS on Port 9200, TLS on 9300, and bind to private cluster network.",
                hardeningGuideFa = "ماژول xpack.security را فعال کرده، روی پورت ۹۲۰۰ پروتکل HTTPS و روی پورت ۹۳۰۰ رمزنگاری TLS کلاستر را پیکربندی نمایید.",
                hardeningGuideRu = "Включите xpack.security.enabled = true. Настройте HTTPS на 9200 и TLS на 9300."
            )
            102 -> PortSecurityDossier(
                port = 102,
                rfcStandard = "ISO on TCP / RFC 1006 (Siemens S7comm)",
                categoryKey = "cat_industrial",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Unauthenticated PLC stop/run commands, Ladder logic readout/injection, Replay attacks on industrial controllers.",
                attackVectorsFa = "ارسال فرامین کنترلی به PLCهای صنعتی زیمنس بدون احراز هویت، دستکاری و تزریق لاجیک‌های نردبانی (Ladder) و حملات بازپخش.",
                attackVectorsRu = "Внедрение команд остановки/запуска PLC без аутентификации, перезапись логики контроллеров Siemens S7, атаки воспроизведения.",
                cveReferences = listOf("CVE-2019-19294", "CVE-2020-15782 (Siemens S7)", "S7comm Packet Injection"),
                hardeningGuideEn = "Migrate to S7-1500 with TLS Secure PG/PC communication. Enforce industrial firewall boundary and restrict engineering station IPs.",
                hardeningGuideFa = "از پی‌ال‌سی‌های سری S7-1500 با قابلیت ارتباط امن TLS استفاده کرده و دسترسی را با فایروال صنعتی به سیستم مهندسی محدود نمایید.",
                hardeningGuideRu = "Используйте S7-1500 с защищенной связью TLS. Изолируйте сеть ПЛК промышленным межсетевым экраном."
            )
            1194 -> PortSecurityDossier(
                port = 1194,
                rfcStandard = "OpenVPN Community / RFC 3948",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Compromised client certificates, Weak cryptographic ciphers, VORACLE compression attack, UDP amplification.",
                attackVectorsFa = "افشای کلیدها و گواهینامه‌های کلاینت، استفاده از الگوریتم‌های رمزنگاری ضعیف، حملات VORACLE ناشی از فشرده‌سازی و حملات منع سرویس UDP.",
                attackVectorsRu = "Компрометация клиентских сертификатов, слабые алгоритмы шифрования, атака VORACLE, UDP-амплификация.",
                cveReferences = listOf("CVE-2024-27459", "CVE-2020-15078", "VORACLE (CVE-2018-3972)"),
                hardeningGuideEn = "Enforce TLS-Crypt HMAC signatures, AES-256-GCM cipher, disable compression (comp-lzo no), and require MFA for user profiles.",
                hardeningGuideFa = "قابلیت tls-crypt را فعال کرده، الگوریتم AES-256-GCM را اجباری و فشرده‌سازی ترافیک را غیرفعال کنید.",
                hardeningGuideRu = "Включите tls-crypt, используйте AES-256-GCM, отключите сжатие и включите MFA для профилей."
            )
            1883, 8883 -> PortSecurityDossier(
                port = port,
                rfcStandard = if (port == 8883) "OASIS MQTT v5.0 over TLS" else "OASIS MQTT v3.1.1 / ISO 20922",
                categoryKey = "cat_iot",
                tierKey = tier,
                encryptionKey = if (port == 8883) "enc_tls" else "enc_cleartext",
                attackVectorsEn = "Unauthenticated broker subscription (# wildcard), Telemetry spoofing, Remote actuator actuation, IoT botnet recruitment.",
                attackVectorsFa = "عضویت در تمام موضوعات (Topic) با کاراکتر جانشین # بدون احراز هویت، جعل داده‌های حسگرها و ارسال فرامین مخرب به عملگرهای اینترنت اشیا.",
                attackVectorsRu = "Подписка на все топики (#) без аутентификации, подмена телеметрии IoT, несанкционированное управление исполнительными механизмами.",
                cveReferences = listOf("CVE-2023-28366 (Mosquitto)", "Unauthenticated MQTT Wildcard Abuse", "IoT Sensor Spoofing"),
                hardeningGuideEn = "Enforce MQTTS (Port 8883) with X.509 client certificates or strong token auth. Set allow_anonymous = false and configure topic ACLs.",
                hardeningGuideFa = "از پورت امن ۸۸۸۳ با گواهینامه TLS استفاده کرده، گزینه allow_anonymous را غیرفعال و برای موضوعات ACL تعریف کنید.",
                hardeningGuideRu = "Используйте защищенный порт 8883 с TLS и сертификатами клиентов. Запретите анонимный доступ (allow_anonymous false) и настройте ACL."
            )
            2049 -> PortSecurityDossier(
                port = 2049,
                rfcStandard = "RFC 7530 / RFC 8881 (Network File System - NFSv4)",
                categoryKey = "cat_files",
                tierKey = tier,
                encryptionKey = "enc_kerberos",
                attackVectorsEn = "Unauthenticated root file export, UID spoofing, World-readable /etc/exports shares, Privilege escalation via SUID binaries.",
                attackVectorsFa = "اشتراک‌گذاری دایرکتوری‌های حساس با دسترسی root، جعل شناسه کاربری (UID)، پیکربندی ناامن no_root_squash و سرقت اسناد محرمانه.",
                attackVectorsRu = "Экспорт файловых систем без аутентификации, подделка UID, эксплуатация опции no_root_squash для повышения привилегий.",
                cveReferences = listOf("CVE-2022-24448 (NFSv4)", "no_root_squash SUID Abuse", "Insecure /etc/exports"),
                hardeningGuideEn = "Enforce NFSv4 with Kerberos RPCSEC_GSS authentication (sec=krb5p). Never export shares with no_root_squash to untrusted networks.",
                hardeningGuideFa = "از پروتکل NFSv4 به همراه احراز هویت Kerberos (sec=krb5p) استفاده کرده و گزینه no_root_squash را غیرفعال نمایید.",
                hardeningGuideRu = "Используйте NFSv4 с аутентификацией Kerberos (sec=krb5p). Никогда не используйте no_root_squash для недоверенных подсетей."
            )
            2375, 2376 -> PortSecurityDossier(
                port = port,
                rfcStandard = "Docker Engine Remote REST API",
                categoryKey = "cat_cloud",
                tierKey = tier,
                encryptionKey = if (port == 2376) "enc_tls" else "enc_cleartext",
                attackVectorsEn = "Unauthenticated Docker API container creation, Host filesystem mount (/host root escape), Cryptomining deployment.",
                attackVectorsFa = "اجرای کانتینرهای با دسترسی بالا (Privileged) بدون احراز هویت، مونت کردن ریشه سیستم‌عامل میزبان و تسخیر کامل سرور.",
                attackVectorsRu = "Создание привилегированных контейнеров без аутентификации, побег из контейнера через монтирование корня хоста (/), криптомайнинг.",
                cveReferences = listOf("CVE-2019-5736 (runc escape)", "CVE-2024-21626 (Leaky Vessels)", "Exposed Unprotected Docker API"),
                hardeningGuideEn = "Never expose Port 2375 publicly. Enforce mutual TLS (mTLS) with client certificates on Port 2376 or bind exclusively to local Unix socket.",
                hardeningGuideFa = "هرگز پورت ۲۳۷۵ را روی شبکه عمومی باز نکنید. ارتباط با داکر را به سوکت یونیکس /var/run/docker.sock یا پورت ۲۳۷۶ همراه با mTLS محدود سازید.",
                hardeningGuideRu = "Никогда не открывайте порт 2375 наружу. Используйте взаимный mTLS с сертификатами на порту 2376 или локальный сокет Unix."
            )
            2379, 2380 -> PortSecurityDossier(
                port = port,
                rfcStandard = "etcd Distributed Key-Value Store",
                categoryKey = "cat_cloud",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Unauthenticated Kubernetes cluster secret extraction, Cluster state manipulation, Complete cluster takeover.",
                attackVectorsFa = "استخراج تمامی توکن‌ها و سکرت‌های کلاستر کوبرنتیز بدون نیاز به رمز، دستکاری وضعیت نودها و نفوذ به زیرساخت ابری.",
                attackVectorsRu = "Выгрузка секретов и токенов Kubernetes без аутентификации, модификация состояния кластера, полный захват инфраструктуры.",
                cveReferences = listOf("CVE-2020-15824", "CVE-2021-28235", "Unauthenticated etcd Cluster Exposure"),
                hardeningGuideEn = "Enforce mutual TLS (mTLS) authentication across all peer and client communication. Restrict access strictly to Kubernetes control plane.",
                hardeningGuideFa = "احراز هویت دوجانبه mTLS را در تمامی سطوح etcd فعال کرده و دسترسی را فقط به کنترل پلین کوبرنتیز محدود نمایید.",
                hardeningGuideRu = "Включите взаимный mTLS для всех узлов и клиентов etcd. Ограничьте доступ строго компонентами control-plane Kubernetes."
            )
            3389 -> PortSecurityDossier(
                port = 3389,
                rfcStandard = "MS-RDP (Remote Desktop Protocol)",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "BlueKeep pre-authentication RCE, Credential brute-forcing, Man-in-the-Middle credential interception, Ransomware entry point.",
                attackVectorsFa = "حملات اجرای کد از راه دور BlueKeep، حدس گذرواژه‌های کاربران ویندوز (Brute-force) و ورودی اصلی حملات باج‌افزاری به سازمان‌ها.",
                attackVectorsRu = "RCE до аутентификации BlueKeep, брутфорс паролей RDP, перехват учетных записей через MITM, основной вектор проникновения вымогателей.",
                cveReferences = listOf("CVE-2019-0708 (BlueKeep)", "CVE-2020-0609 (RD Gateway)", "CVE-2022-21893"),
                hardeningGuideEn = "Enforce Network Level Authentication (NLA). Place RDP behind WireGuard/IPsec VPN or RD Gateway with MFA. Implement account lockout.",
                hardeningGuideFa = "قابلیت احراز هویت در سطح شبکه (NLA) را فعال کرده، پورت ۳۳۸۹ را در فایروال بسته و دسترسی را از طریق VPN همراه با احراز هویت دوعاملی برقرار کنید.",
                hardeningGuideRu = "Включите Network Level Authentication (NLA). Разместите RDP за VPN или RD Gateway с MFA. Настройте блокировку учетных записей."
            )
            4840 -> PortSecurityDossier(
                port = 4840,
                rfcStandard = "OPC UA (IEC 62541)",
                categoryKey = "cat_industrial",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "SecurityPolicy 'None' plaintext sniffing, Unsigned certificate trust, SCADA telemetry alteration, Buffer overflow.",
                attackVectorsFa = "سوءاستفاده از سیاست امنیتی 'None' برای شنود ترافیک صنعتی، پذیرش گواهینامه‌های نامعتبر و دستکاری فرآیندهای اتوماسیون.",
                attackVectorsRu = "Перехват незашифрованного трафика при политике 'None', доверие неподписанным сертификатам, подмена телеметрии SCADA.",
                cveReferences = listOf("CVE-2022-29866", "CVE-2021-36756", "OPC UA SecurityPolicy None"),
                hardeningGuideEn = "Disable SecurityPolicy 'None'. Enforce Basic256Sha256 or Aes128_Sha256_RsaOaep with mutual X.509 application certificates.",
                hardeningGuideFa = "سیاست SecurityPolicy=None را غیرفعال کرده و از الگوریتم Basic256Sha256 با گواهینامه‌های X.509 معتبر استفاده کنید.",
                hardeningGuideRu = "Отключите политику SecurityPolicy 'None'. Включите Basic256Sha256 с взаимными сертификатами X.509."
            )
            5555 -> PortSecurityDossier(
                port = 5555,
                rfcStandard = "ADB (Android Debug Bridge Network Transport)",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Unauthenticated root ADB shell access, Malicious APK sideloading, Screen capture, Device bricking.",
                attackVectorsFa = "دسترسی مستقیم به شل روت دستگاه بدون رمز، نصب بی‌اجازه بدافزارها و برنامه‌های جاسوسی، ضبط صفحه و سرقت اطلاعات حافظه.",
                attackVectorsRu = "Прямой доступ к root-шелу без аутентификации, скрытая установка вредоносных APK, захват экрана и кража данных.",
                cveReferences = listOf("Exposed ADB Network Port", "ADB Miner Botnet", "CVE-2020-0041"),
                hardeningGuideEn = "Disable Network ADB on production devices (setprop service.adb.tcp.port -1). Require USB debugging with RSA key authorization.",
                hardeningGuideFa = "قابلیت دیباگ از طریق شبکه را غیرفعال کرده و دیباگ را فقط به کابل USB همراه با تایید کلید RSA محدود سازید.",
                hardeningGuideRu = "Отключите отладку по сети на рабочих устройствах. Используйте только отладку по USB с RSA-авторизацией."
            )
            5672 -> PortSecurityDossier(
                port = 5672,
                rfcStandard = "OASIS AMQP 0-9-1 / AMQP 1.0 (RabbitMQ)",
                categoryKey = "cat_infra",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Default guest:guest credentials, Message interception, Unauthorized queue consumption, Message injection.",
                attackVectorsFa = "سوءاستفاده از حساب پیش‌فرض guest، شنود و سرقت پیام‌های صَف، تزریق تسک‌های مخرب و دستکاری ارتباطات بین میکروسرویس‌ها.",
                attackVectorsRu = "Использование стандартного логина guest:guest, перехват сообщений в очередях, внедрение вредоносных задач.",
                cveReferences = listOf("CVE-2023-46118", "CVE-2021-22116", "RabbitMQ Default Credential Exposure"),
                hardeningGuideEn = "Delete default 'guest' user or restrict to loopback. Enforce AMQPS (Port 5671) with TLS 1.3 and granular virtual host permissions.",
                hardeningGuideFa = "کاربر پیش‌فرض guest را حذف کرده و از پورت امن ۵۶۷۱ همراه با رمزنگاری TLS و تعیین دسترسی مجزا برای هر vhost استفاده نمایید.",
                hardeningGuideRu = "Удалите стандартного пользователя 'guest'. Перейдите на защищенный порт AMQPS (5671) с TLS 1.3 и разделением прав на vhost."
            )
            5683, 5684 -> PortSecurityDossier(
                port = port,
                rfcStandard = if (port == 5684) "RFC 7252 (CoAP over DTLS)" else "RFC 7252 (Constrained Application Protocol)",
                categoryKey = "cat_iot",
                tierKey = tier,
                encryptionKey = if (port == 5684) "enc_tls" else "enc_cleartext",
                attackVectorsEn = "UDP reflection amplification DDoS, Sensor manipulation, Unauthenticated IoT device actuation.",
                attackVectorsFa = "حملات منع سرویس تقویت‌شده بازتابی UDP، دستکاری داده‌های حسگرهای با منابع محدود و ارسال دستورات ساختگی.",
                attackVectorsRu = "Атаки UDP Amplification DDoS, подмена показаний микроконтроллеров, несанкционированное управление датчиками.",
                cveReferences = listOf("CVE-2018-1000851", "CoAP UDP Amplification", "IoT Constrained Node Exploits"),
                hardeningGuideEn = "Enforce CoAPS on Port 5684 with DTLS (Pre-Shared Key or Raw Public Key). Rate-limit UDP packets at perimeter firewalls.",
                hardeningGuideFa = "از نسخه امن CoAPS روی پورت ۵۶۸۴ با رمزنگاری DTLS استفاده کرده و نرخ بسته‌های UDP را در فایروال محدود کنید.",
                hardeningGuideRu = "Используйте CoAPS на порту 5684 с шифрованием DTLS. Настройте ограничение скорости UDP-запросов на брандмауэре."
            )
            6443 -> PortSecurityDossier(
                port = 6443,
                rfcStandard = "Kubernetes API Server Control Plane",
                categoryKey = "cat_cloud",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Anonymous API access, RBAC privilege escalation, Kubelet proxy command execution, Cluster compromise.",
                attackVectorsFa = "دسترسی ناشناس به API سرور کوبرنتیز، ارتقای سطح دسترسی از طریق نقش‌های RBAC، اجرای فرامین در پادها و کنترل کامل کلاستر.",
                attackVectorsRu = "Анонимный доступ к API Kubernetes, эскалация привилегий через RBAC, выполнение команд внутри подов через kubelet proxy.",
                cveReferences = listOf("CVE-2018-1002105 (Billion Laughs / Proxy Escape)", "CVE-2022-3172", "CVE-2023-2727"),
                hardeningGuideEn = "Set --anonymous-auth=false. Restrict API access to internal management IPs. Enforce strict RBAC with least privilege.",
                hardeningGuideFa = "گزینه anonymous-auth را غیرفعال کرده، دسترسی به پورت ۶۴۴۳ را به آی‌پی‌های مدیریت محدود و سیاست‌های سخت‌گیرانه RBAC اعمال کنید.",
                hardeningGuideRu = "Отключите анонимный доступ (--anonymous-auth=false). Ограничьте доступ доверенными IP-адресами и настройте строгий RBAC."
            )
            8500 -> PortSecurityDossier(
                port = 8500,
                rfcStandard = "HashiCorp Consul HTTP API & Service Mesh",
                categoryKey = "cat_cloud",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Consul Exec RCE via unauthenticated script checks, KV store secret tampering, Service discovery spoofing.",
                attackVectorsFa = "اجرای کد از راه دور با سوءاستفاده از اسکریپت‌های Consul Exec، دستکاری کلیدها و مقادیر در مخزن KV و جعل مسیرهای دیسکاوری.",
                attackVectorsRu = "RCE через Consul Exec и проверки скриптов без аутентификации, подмена конфигураций в KV-хранилище, спуфинг сервисов.",
                cveReferences = listOf("CVE-2018-19653 (Consul Exec RCE)", "CVE-2020-13250", "Unauthenticated Consul KV Abuse"),
                hardeningGuideEn = "Set enable_script_checks = false. Enforce ACL system with default 'deny' policy and configure HTTPS with client certificates.",
                hardeningGuideFa = "قابلیت enable_script_checks را غیرفعال کرده، سیستم ACL با سیاست پیش‌فرض رد دسترسی (Deny) را فعال و HTTPS را اجباری کنید.",
                hardeningGuideRu = "Установите enable_script_checks = false. Включите систему ACL с политикой 'deny' по умолчанию и используйте HTTPS."
            )
            10250 -> PortSecurityDossier(
                port = 10250,
                rfcStandard = "Kubernetes Kubelet HTTPS Worker Node API",
                categoryKey = "cat_cloud",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Unauthenticated pod/container execution (/exec), Node credential exfiltration, Worker node compromise.",
                attackVectorsFa = "اجرای فرامین در کانتینرها از طریق اندپوینت /exec در کوبلت بدون لاگین، سرقت توکن‌های نود و نفوذ به لایه کارگری کلاستر.",
                attackVectorsRu = "Выполнение команд в контейнерах через эндпоинт /exec без авторизации, кража учетных данных ноды, компрометация воркера.",
                cveReferences = listOf("Unauthenticated Kubelet 10250 Exposure", "CVE-2021-25741", "Kubernetes Pod Escape"),
                hardeningGuideEn = "Set anonymous.enabled = false and authorization.mode = Webhook in kubelet-config. Block WAN access to Port 10250.",
                hardeningGuideFa = "دسترسی ناشناس را در پیکربندی کوبلت غیرفعال کرده و احراز هویت Webhook را فعال کنید. این پورت نباید به خارج از کلاستر باز باشد.",
                hardeningGuideRu = "Отключите анонимный доступ (anonymous.enabled: false) и включите режим Webhook. Закройте порт 10250 снаружи."
            )
            11211 -> PortSecurityDossier(
                port = 11211,
                rfcStandard = "Memcached Text/Binary Protocol",
                categoryKey = "cat_database",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Memcrashed UDP reflection DDoS (50,000x amplification), Unauthenticated cache dumping, Cache poisoning.",
                attackVectorsFa = "حملات ویرانگر منع سرویس بازتابی UDP (تقویت تا ۵۰ هزار برابر)، استخراج کامل اطلاعات کش‌شده و مسموم‌سازی نشست‌های کاربران.",
                attackVectorsRu = "Атаки Memcrashed UDP Amplification (усиление до 50 000 раз), выгрузка кэша без аутентификации, подмена данных сессий.",
                cveReferences = listOf("CVE-2018-1000115 (Memcrashed DDoS)", "CVE-2016-8704", "Cleartext Memcached Exposure"),
                hardeningGuideEn = "Disable UDP listener (-U 0). Bind Memcached strictly to 127.0.0.1 and enforce SASL authentication if networked.",
                hardeningGuideFa = "شنونده پروتکل UDP را با پارامتر U 0- غیرفعال کرده و مم‌کش را منحصراً به 127.0.0.1 متصل نمایید.",
                hardeningGuideRu = "Отключите UDP (-U 0). Привяжите Memcached строго к 127.0.0.1. При сетевом доступе используйте аутентификацию SASL."
            )
            47808 -> PortSecurityDossier(
                port = 47808,
                rfcStandard = "BACnet/IP (ANSI/ASHRAE 135 / ISO 16484-5)",
                categoryKey = "cat_industrial",
                tierKey = tier,
                encryptionKey = "enc_cleartext",
                attackVectorsEn = "Unauthenticated building automation control, HVAC & chiller manipulation, Fire alarm / access control interference.",
                attackVectorsFa = "کنترل سیستم‌های مدیریت هوشمند ساختمان (BMS) بدون احراز هویت، دستکاری سیستم‌های تهویه و سرمایش/گرمایش، ایجاد اختلال در سامانه‌های امنیتی.",
                attackVectorsRu = "Несанкционированное управление автоматизацией зданий (BMS), манипуляция системами вентиляции/HVAC, отключение сигнализаций.",
                cveReferences = listOf("ICS-CERT BACnet Advisories", "Cleartext Building Automation", "CVE-2020-10640"),
                hardeningGuideEn = "Isolate BACnet traffic in a dedicated building management VLAN. Deploy BACnet/SC (Secure Connect) with TLS and block UDP 47808 at boundary.",
                hardeningGuideFa = "ترافیک BACnet را در VLAN اختصاصی مدیریت ساختمان ایزوله کرده و از استاندارد امن BACnet/SC به همراه رمزنگاری TLS استفاده نمایید.",
                hardeningGuideRu = "Изолируйте BACnet в выделенном VLAN. Перейдите на защищенный стандарт BACnet/SC с TLS. Заблокируйте UDP 47808 на периметре."
            )
            51820 -> PortSecurityDossier(
                port = 51820,
                rfcStandard = "WireGuard VPN (Noise Protocol Framework)",
                categoryKey = "cat_remote",
                tierKey = tier,
                encryptionKey = "enc_tls",
                attackVectorsEn = "Compromised private keys, Traffic volume side-channel analysis, Misconfigured AllowedIPs routing leaks.",
                attackVectorsFa = "سرقت کلیدهای خصوصی کلاینت یا سرور، تحلیل حجم ترافیک و نشت پکت‌ها در صورت تنظیم نادرست روتینگ AllowedIPs.",
                attackVectorsRu = "Компрометация приватных ключей WireGuard, анализ объемов зашифрованного трафика, утечка маршрутов при неверном AllowedIPs.",
                cveReferences = listOf("CVE-2021-41072", "Noise Protocol Security Architecture"),
                hardeningGuideEn = "Enforce PresharedKey (PSK) for post-quantum resistance. Regularly rotate keypairs and restrict endpoint listening ports via firewall.",
                hardeningGuideFa = "از کلید مشترک تکمیلی (PresharedKey) برای محافظت پساکوانتومی استفاده کرده و کلیدها را به صورت دوره‌ای جایگزین کنید.",
                hardeningGuideRu = "Используйте PresharedKey (PSK) для защиты. Регулярно меняйте пары ключей и ограничьте доступ по IP на брандмауэره."
            )
            else -> PortSecurityDossier(
                port = port,
                rfcStandard = "IANA Port $port / Standard Network Protocol",
                categoryKey = if (port in listOf(80, 443, 8000, 8080, 8443)) "cat_web" else "cat_general",
                tierKey = tier,
                encryptionKey = if (port in listOf(443, 8443, 9443, 22)) "enc_tls" else "enc_cleartext",
                attackVectorsEn = "Unauthorized service interaction, Banner fingerprinting, Protocol fuzzing, Potential unauthenticated exposure.",
                attackVectorsFa = "بررسی اطلاعات سرویس، ارسال داده‌های مخرب (Fuzzing) و امکان دسترسی غیرمجاز در صورت ضعف در احراز هویت.",
                attackVectorsRu = "Несанкционированное взаимодействие со службой, фаззинг протокола, потенциальный неаутентифицированный доступ.",
                cveReferences = listOf("Generic Service Vulnerabilities", "CVE / NVD Embedded Intel"),
                hardeningGuideEn = "Restrict access to authorized IP addresses. Ensure underlying application is patched and uses encrypted transport.",
                hardeningGuideFa = "دسترسی به پورت را از طریق فایروال به آی‌پی‌های مجاز محدود کرده و نسخه‌های نرم‌افزاری را به‌روزرسانی کنید.",
                hardeningGuideRu = "Ограничьте доступ доверенными IP-адресами на брандмауэре. Убедитесь в актуальности версий ПО."
            )
        }
    }
}
