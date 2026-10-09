package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.zIndex
import com.mrcoder20.portx.presentation.ui.components.DisintegrationContainer
import com.mrcoder20.portx.presentation.ui.components.LaunchedAutoScrollHint
import com.mrcoder20.portx.presentation.ui.components.LocalTouchEmulation
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.cyberPulse
import com.mrcoder20.portx.presentation.ui.components.horizontalFadingEdges
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.components.touchDragScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.domain.PortIntelligence
import com.mrcoder20.portx.domain.PortSecurityDossier
import com.mrcoder20.portx.presentation.ui.theme.*
import com.mrcoder20.portx.presentation.viewmodel.ScanViewModel
import com.mrcoder20.portx.presentation.viewmodel.ScanUIState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// --- Data Models & Helpers ---

data class DisplayPort(val number: Int, val title: String, val description: String, val color: Color)

val KNOWN_PORTS_SET = setOf(
    20, 21, 22, 23, 25, 53, 67, 68, 69, 80, 102, 110, 123, 135, 137, 138, 139, 143, 161, 162, 389,
    443, 445, 465, 502, 514, 515, 548, 587, 631, 636, 993, 995, 1194, 1433, 1521, 1700, 1723, 1812, 1813,
    1883, 1884, 1900, 2049, 2181, 2375, 2376, 2379, 2380, 3000, 3306, 3389, 4222, 4840, 5000, 5060, 5061,
    5432, 5555, 5672, 5683, 5684, 5900, 6379, 6443, 8000, 8080, 8081, 8088, 8123, 8200, 8443, 8500, 8883,
    8888, 9000, 9042, 9090, 9092, 9100, 9200, 9300, 10250, 11211, 11311, 27017, 47808, 50051, 51820
)

fun isIdentifiedPort(port: Int, rawService: String? = null, banner: String? = null): Boolean {
    if (port in KNOWN_PORTS_SET) return true
    if (!rawService.isNullOrBlank() && !rawService.equals("unknown", ignoreCase = true)) return true
    if (!banner.isNullOrBlank()) return true
    return false
}

fun getPortColor(port: Int, accent: Color): Color {
    return when (port) {
        // High-risk, unencrypted, industrial or dangerous vectors
        21, 23, 102, 135, 137, 138, 139, 445, 502, 1883, 2049, 2181, 2375, 2379, 5683, 47808, 5555, 10250, 11211 -> DangerNeon
        // High-privilege / Admin / Database / Remote access / VPN
        22, 1194, 1433, 1521, 3306, 3389, 4840, 51820, 5432, 5900, 6379, 6443, 8123, 8200, 8500, 9042, 9200, 9300, 27017 -> SecondaryNeon
        // Standard Web / HTTPS / Proxy
        80, 443, 8000, 8080, 8081, 8088, 8443, 8888, 9090, 3000, 5000 -> accent
        // Standard Low-risk / Infrastructure
        53, 123 -> TertiaryNeon
        else -> TertiaryNeon
    }
}

fun getServiceTitle(port: Int, rawService: String? = null, lang: String = "en"): String {
    if (lang == "fa") {
        return when (port) {
            21 -> "انتقال فایل FTP"
            22 -> "ترمینال امن SSH"
            23 -> "ترمینال ریموت Telnet"
            25 -> "رله ایمیل SMTP"
            53 -> "سرور نام دامنه DNS"
            67, 68 -> "پیکربندی شبکه DHCP"
            69 -> "انتقال فایل TFTP"
            80 -> "وب‌سرور HTTP"
            102 -> "کنترلر صنعتی Siemens S7comm"
            110 -> "سرور ایمیل POP3"
            123 -> "همگام‌سازی زمان NTP"
            135 -> "اندپوینت MS RPC ویندوز"
            137, 138, 139 -> "سرویس نشست NetBIOS"
            143 -> "سرور ایمیل IMAP"
            161, 162 -> "عامل مانیتورینگ SNMP"
            389 -> "سرویس دایرکتوری LDAP"
            443 -> "وب‌سرور امن HTTPS"
            445 -> "اشتراک فایل SMB / اکتیو دایرکتوری"
            465 -> "ایمیل امن SMTPS"
            502 -> "پروتکل کنترل صنعتی Modbus"
            514 -> "عامل ثبت لاگ Syslog"
            515 -> "سرویس چاپ شبکه LPD"
            548 -> "اشتراک فایل اپل AFP"
            587 -> "ارسال ایمیل SMTP STARTTLS"
            631 -> "سرویس پرینت اینترنتی IPP"
            636 -> "دایرکتوری امن LDAPS"
            993 -> "ایمیل امن IMAPS"
            995 -> "ایمیل امن POP3S"
            1194 -> "سرور وی‌پی‌ان OpenVPN"
            1433 -> "پایگاه‌داده Microsoft SQL"
            1521 -> "پایگاه‌داده Oracle"
            1700 -> "گیت‌وی اینترنت اشیا LoRaWAN"
            1723 -> "تونل وی‌پی‌ان PPTP"
            1812, 1813 -> "احراز هویت و حسابداری RADIUS"
            1883 -> "بروکر اینترنت اشیا MQTT"
            1884 -> "گیت‌وی MQTT-SN"
            1900 -> "کشف سرویس SSDP / UPnP"
            2049 -> "اشتراک فایل شبکه NFS"
            2181 -> "نود کلاستر Apache ZooKeeper"
            2375, 2376 -> "کنترلر کانتینر Docker API"
            2379, 2380 -> "پایگاه کلید-مقدار etcd"
            3000 -> "داشبورد Grafana / وب‌اپ"
            3306 -> "پایگاه‌داده MySQL / MariaDB"
            3389 -> "ریموت دسکتاپ ویندوز RDP"
            4222 -> "بروکر پیام‌رسان NATS"
            4840 -> "سرور صنعتی OPC UA"
            5000 -> "وب‌سرویس / مدیا سرور UPnP"
            5060, 5061 -> "سیگنالینگ تلفن اینترنتی SIP VoIP"
            5432 -> "پایگاه‌داده پیشرفته PostgreSQL"
            5555 -> "پل اشکال‌زدایی اندروید ADB"
            5672 -> "بروکر صف پیام RabbitMQ"
            5683, 5684 -> "نود حسگر اینترنت اشیا CoAP"
            5900 -> "ریموت دسکتاپ گرافیکی VNC"
            6379 -> "پایگاه‌داده حافظه‌محور Redis"
            6443 -> "کنترل پلین کلاستر Kubernetes API"
            8000 -> "وب‌سرویس جانبی HTTP"
            8080 -> "پراکسی / وب‌سرور ثانویه HTTP"
            8081, 8088 -> "وب‌سرور توسعه HTTP"
            8123 -> "پایگاه‌داده تحلیلی ClickHouse"
            8200 -> "مدیریت کلید و اسرار HashiCorp Vault"
            8443 -> "وب‌سرور امن ثانویه HTTPS"
            8500 -> "سرویس کشف و رجیستری Consul"
            8883 -> "ارتباط امن اینترنت اشیا MQTTS"
            8888, 9090 -> "کنسول وب یا پرومتئوس HTTP"
            9000 -> "سرور تحلیل کد SonarQube / MinIO S3"
            9042 -> "پایگاه‌داده توزیع‌شده Apache Cassandra"
            9092 -> "بروکر جریان داده Apache Kafka"
            9100 -> "پورت خام پرینتر JetDirect"
            9200, 9300 -> "کلاستر موتور جستجو Elasticsearch"
            10250 -> "عامل نود Kubernetes Kubelet"
            11211 -> "کش سریع حافظه‌محور Memcached"
            11311 -> "نود مرکزی رباتیک ROS Master"
            27017 -> "پایگاه‌داده اسنادی MongoDB"
            47808 -> "اتوماسیون هوشمند ساختمان BACnet"
            50051 -> "میکروسرویس پرسرعت gRPC"
            51820 -> "تونل رمزنگاری‌شده WireGuard VPN"
            else -> {
                if (!rawService.isNullOrBlank() && rawService != "unknown") {
                    "سرویس $rawService"
                } else {
                    "سرویس پورت $port"
                }
            }
        }
    } else if (lang == "ru") {
        return when (port) {
            21 -> "Передача файлов FTP"
            22 -> "Защищенная оболочка SSH"
            23 -> "Удаленная оболочка Telnet"
            25 -> "Почтовый релей SMTP"
            53 -> "Сервер имен DNS"
            67, 68 -> "Сетевая конфигурация DHCP"
            69 -> "Простой протокол TFTP"
            80 -> "Веб-сервер HTTP"
            102 -> "ПЛК Siemens S7comm"
            110 -> "Почтовый сервер POP3"
            123 -> "Синхронизация времени NTP"
            135 -> "Сопоставитель конечных точек MS RPC"
            137, 138, 139 -> "Служба сеансов NetBIOS"
            143 -> "Почтовый сервер IMAP"
            161, 162 -> "Агент сетевого мониторинга SNMP"
            389 -> "Служба каталогов LDAP"
            443 -> "Защищенный веб-сервер HTTPS"
            445 -> "Общий доступ SMB / Active Directory"
            465 -> "Защищенная почта SMTPS"
            502 -> "Промышленный протокол Modbus"
            514 -> "Агент системного журнала Syslog"
            515 -> "Служба сетевой печати LPD"
            548 -> "Файловая служба Apple AFP"
            587 -> "Отправка почты SMTP STARTTLS"
            631 -> "Служба интернет-печати IPP"
            636 -> "Защищенный каталог LDAPS"
            993 -> "Защищенная почта IMAPS"
            995 -> "Защищенная почта POP3S"
            1194 -> "Сервер OpenVPN"
            1433 -> "СУБД Microsoft SQL Server"
            1521 -> "СУБД Oracle Database"
            1700 -> "Шлюз IoT LoRaWAN"
            1723 -> "VPN-туннель PPTP"
            1812, 1813 -> "Аутентификация RADIUS"
            1883 -> "Брокер IoT MQTT"
            1884 -> "Шлюз MQTT-SN"
            1900 -> "Обнаружение служб SSDP / UPnP"
            2049 -> "Сетевая файловая система NFS"
            2181 -> "Узел кластера ZooKeeper"
            2375, 2376 -> "API демона Docker"
            2379, 2380 -> "Хранилище etcd"
            3000 -> "Дашборд Grafana / Веб-приложение"
            3306 -> "СУБД MariaDB / MySQL"
            3389 -> "Удаленный рабочий стол RDP"
            4222 -> "Брокер сообщений NATS"
            4840 -> "Промышленный сервер OPC UA"
            5000 -> "Веб-служба / UPnP"
            5060, 5061 -> "IP-телефония SIP VoIP"
            5432 -> "СУБД PostgreSQL"
            5555 -> "Отладчик Android ADB"
            5672 -> "Брокер очередей RabbitMQ"
            5683, 5684 -> "Узел датчиков CoAP IoT"
            5900 -> "Удаленный дисплей VNC"
            6379 -> "Хранилище в памяти Redis"
            6443 -> "API сервера Kubernetes"
            8000 -> "Веб-служба HTTP"
            8080 -> "Прокси / Веб-сервер HTTP"
            8081, 8088 -> "Альтернативный веб-сервер HTTP"
            8123 -> "Аналитическая СУБД ClickHouse"
            8200 -> "Управление секретами HashiCorp Vault"
            8443 -> "Защищенный веб-сервер HTTPS Alt"
            8500 -> "Реестр служб Consul"
            8883 -> "Защищенный брокер MQTTS"
            8888, 9090 -> "Веб-консоль или Prometheus"
            9000 -> "Анализ кода SonarQube / MinIO S3"
            9042 -> "СУБД Apache Cassandra"
            9092 -> "Потоковый брокер Apache Kafka"
            9100 -> "Прямой порт принтера JetDirect"
            9200, 9300 -> "Кластер поиска Elasticsearch"
            10250 -> "Агент узла Kubelet"
            11211 -> "Кэш-память Memcached"
            11311 -> "Главный узел робототехники ROS"
            27017 -> "СУБД MongoDB"
            47808 -> "Автоматизация зданий BACnet"
            50051 -> "Микрослужба gRPC"
            51820 -> "VPN-туннель WireGuard"
            else -> {
                if (!rawService.isNullOrBlank() && rawService != "unknown") {
                    "Служба $rawService"
                } else {
                    "Служба на порту $port"
                }
            }
        }
    }
    return when (port) {
        21 -> "FTP File Transfer"
        22 -> "SSH Secure Shell"
        23 -> "Telnet Remote Shell"
        25 -> "SMTP Mail Relay"
        53 -> "DNS Name Server"
        67, 68 -> "DHCP Network Config"
        69 -> "TFTP Trivial FTP"
        80 -> "HTTP Web Server"
        102 -> "Siemens S7comm PLC"
        110 -> "POP3 Mail Server"
        123 -> "NTP Time Sync"
        135 -> "MS RPC Endpoint Mapper"
        137, 138, 139 -> "NetBIOS Session Service"
        143 -> "IMAP Mail Server"
        161, 162 -> "SNMP Network Agent"
        389 -> "LDAP Directory Service"
        443 -> "HTTPS Secure Web"
        445 -> "SMB / Active Directory"
        465 -> "SMTPS Secure Mail"
        502 -> "Modbus Industrial ICS"
        514 -> "Syslog Logging Agent"
        515 -> "LPD Line Printer"
        548 -> "AFP Apple Filing"
        587 -> "SMTP Submission"
        631 -> "IPP Internet Printing"
        636 -> "LDAPS Secure Directory"
        993 -> "IMAPS Secure Mail"
        995 -> "POP3S Secure Mail"
        1194 -> "OpenVPN Server"
        1433 -> "Microsoft SQL Server"
        1521 -> "Oracle Database"
        1700 -> "LoRaWAN Gateway"
        1723 -> "PPTP VPN Tunnel"
        1812, 1813 -> "RADIUS Auth/Acct"
        1883 -> "MQTT IoT Broker"
        1884 -> "MQTT-SN Gateway"
        1900 -> "SSDP UPnP Discovery"
        2049 -> "NFS Network Share"
        2181 -> "ZooKeeper Cluster Node"
        2375, 2376 -> "Docker Daemon API"
        2379, 2380 -> "etcd Datastore"
        3000 -> "Grafana / React Web App"
        3306 -> "MariaDB Database"
        3389 -> "RDP Remote Desktop"
        4222 -> "NATS Message Broker"
        4840 -> "OPC UA Industrial"
        5000 -> "UPnP / Web Service"
        5060, 5061 -> "SIP VoIP Signaling"
        5432 -> "PostgreSQL Database"
        5555 -> "Android ADB Debugger"
        5672 -> "RabbitMQ Message Broker"
        5683, 5684 -> "CoAP IoT Node"
        5900 -> "VNC Remote Display"
        6379 -> "Redis In-Memory DB"
        6443 -> "Kubernetes API Server"
        8000 -> "HTTP Web Service"
        8080 -> "HTTP Proxy / Alternate"
        8081, 8088 -> "HTTP Alternate Web"
        8123 -> "ClickHouse Analytical DB"
        8200 -> "HashiCorp Vault"
        8443 -> "HTTPS Secure Alt"
        8500 -> "Consul Service Mesh"
        8883 -> "MQTTS Secure IoT"
        8888, 9090 -> "HTTP Web Console"
        9000 -> "SonarQube / MinIO S3 API"
        9042 -> "Cassandra Database"
        9092 -> "Apache Kafka Broker"
        9100 -> "RAW JetDirect Printer"
        9200, 9300 -> "Elasticsearch Cluster"
        10250 -> "Kubelet Node Agent"
        11211 -> "Memcached Cache"
        11311 -> "ROS Master Node"
        27017 -> "MongoDB Database"
        47808 -> "BACnet Building Automation"
        50051 -> "gRPC Microservice"
        51820 -> "WireGuard VPN Tunnel"
        else -> {
            if (!rawService.isNullOrBlank() && rawService != "unknown") {
                rawService.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Service"
            } else {
                "Service on Port $port"
            }
        }
    }
}

fun getServiceDescription(port: Int, rawService: String? = null, lang: String = "en"): String {
    if (lang == "fa") {
        return when (port) {
            21 -> "پروتکل انتقال فایل غیررمزنگاری‌شده"
            22 -> "ترمینال امن رمزنگاری‌شده و انتقال فایل"
            23 -> "نشست ترمینال از راه دور با متن خام (ناامن)"
            25 -> "رله انتقال ایمیل SMTP"
            53 -> "تفکیک‌کننده و سرور نام دامنه DNS"
            67, 68 -> "پروتکل تخصیص پویای آدرس شبکه DHCP"
            69 -> "پروتکل ساده و بدون احراز هویت انتقال فایل TFTP"
            80 -> "پروتکل استاندارد وب HTTP بدون رمزنگاری"
            102 -> "کنترل‌کننده منطقی برنامه‌پذیر صنعتی زیمنس Step7"
            110 -> "پروتکل دریافت ایمیل نسخه ۳ (POP3)"
            123 -> "همگام‌سازی ساعت شبکه بر بستر NTP"
            135 -> "مکان‌یاب اندپوینت‌های RPC ویندوز مایکروسافت"
            137, 138, 139 -> "تفکیک نام و نشست شبکه محلی NetBIOS"
            143 -> "پروتکل دسترسی به پیام‌های ایمیل IMAP"
            161, 162 -> "پروتکل مدیریت و نظارت بر تجهیزات شبکه SNMP"
            389 -> "سرویس دایرکتوری سبک‌وزن شبکه LDAP"
            443 -> "وب‌سرویس امن رمزنگاری‌شده با پروتکل TLS/SSL"
            445 -> "اشتراک‌گذاری فایل و احراز هویت اکتیو دایرکتوری SMB"
            465 -> "پروتکل امن ارسال ایمیل با لایه رمزنگاری TLS/SSL"
            502 -> "پروتکل صنعتی نظارت و کنترل داده‌های Modbus TCP"
            514 -> "دریافت‌کننده لاگ‌های سیستمی و رویدادهای Syslog"
            515 -> "سرویس مدیریت صف چاپ شبکه Line Printer Daemon"
            548 -> "پروتکل اشتراک فایل سیستم‌های مک‌او‌اس اپل AFP"
            587 -> "ارسال پیام ایمیل با ارتقای امنیتی STARTTLS"
            631 -> "سرویس چاپ تحت شبکه اینترنتی CUPS IPP"
            636 -> "سرویس دایرکتوری امن LDAP بر بستر TLS"
            993 -> "سرویس دریافت ایمیل امن IMAP بر بستر TLS"
            995 -> "سرویس دریافت ایمیل امن POP3 بر بستر TLS"
            1194 -> "دیمن شبکه خصوصی مجازی OpenVPN"
            1433 -> "موتور پایگاه‌داده رابطه‌ای Microsoft SQL Server"
            1521 -> "شنونده شبکه پایگاه‌داده Oracle TNS"
            1700 -> "واسط هدایت بسته‌های شبکه بی‌سیم LoRaWAN"
            1723 -> "پروتکل ایجاد تونل‌های ارتباطی PPTP"
            1812, 1813 -> "سرویس احراز هویت، اعتبارسنجی و حسابداری RADIUS"
            1883 -> "بروکر انتقال تله‌متری اینترنت اشیا MQTT بدون رمزنگاری"
            1884 -> "پل ارتباطی MQTT برای شبکه‌های حسگر IoT"
            1900 -> "پروتکل کشف خودکار سرویس‌ها و تجهیزات SSDP / UPnP"
            2049 -> "سیستم اشتراک فایل‌های شبکه‌ای یونیکس NFS"
            2181 -> "سرویس هماهنگی توزیع‌شده Apache ZooKeeper"
            2375, 2376 -> "رابط برنامه‌نویسی کنترل موتور کانتینری Docker"
            2379, 2380 -> "پایگاه ذخیره‌سازی داده‌های کلید-مقدار توزیع‌شده etcd"
            3000 -> "داشبورد متریک‌های Grafana یا وب‌اپلیکیشن فرانت‌اند"
            3306 -> "پایگاه‌داده رابطه‌ای MySQL / MariaDB"
            3389 -> "ترمینال ریموت دسکتاپ ویندوز Microsoft RDP"
            4222 -> "سیستم پیام‌رسانی با کارایی بالای NATS"
            4840 -> "سرور یکپارچه اتوماسیون صنعتی OPC UA"
            5000 -> "فریم‌ورک وب یا مدیا استریمینگ UPnP"
            5060, 5061 -> "سیگنالینگ صوتی و تصویری تلفن اینترنتی SIP VoIP"
            5432 -> "پایگاه‌داده رابطه‌ای پیشرفته PostgreSQL"
            5555 -> "شنونده دیباگ و مدیریت دستگاه‌های اندروید ADB"
            5672 -> "بروکر صف پیام‌های تجاری پروتکل AMQP (RabbitMQ)"
            5683, 5684 -> "پروتکل کاربردی محدود حسگرهای اینترنت اشیا CoAP"
            5900 -> "ریموت کنترل صفحه دسکتاپ VNC"
            6379 -> "پایگاه ذخیره‌سازی کلید-مقدار در حافظه Redis"
            6443 -> "رابط کنترل مرکزی کلاستر Kubernetes API"
            8000 -> "وب‌سرور یا سرور برنامه جانبی HTTP"
            8080 -> "پراکسی یا شنونده ثانویه پرکاربرد HTTP"
            8081, 8088 -> "پورت جانبی برنامه‌های تحت وب HTTP"
            8123 -> "پورت وب پایگاه‌داده تحلیلی ستونی ClickHouse"
            8200 -> "سرویس مدیریت کلیدها و اسرار HashiCorp Vault"
            8443 -> "وب‌سرویس امن ثانویه رمزنگاری‌شده HTTPS"
            8500 -> "سرویس کشف خدمات و ذخیره مقادیر Consul"
            8883 -> "تله‌متری امن اینترنت اشیا MQTT بر بستر TLS"
            8888, 9090 -> "کنسول وب یا سامانه جمع‌آوری متریک Prometheus"
            9000 -> "سرور ارزیابی کیفیت کد SonarQube یا فضای ذخیره‌سازی MinIO S3"
            9042 -> "انتقال داده‌های بومی CQL پایگاه‌داده توزیع‌شده Cassandra"
            9092 -> "بروکر جریان داده‌های رویدادمحور Apache Kafka"
            9100 -> "کانال خام انتقال داده‌های پرینتر HP JetDirect"
            9200, 9300 -> "موتور جستجو و تحلیل داده‌های توزیع‌شده Elasticsearch"
            10250 -> "رابط برنامه‌نویسی عامل نود Kubernetes Kubelet"
            11211 -> "کش سریع حافظه‌محور اشیا Memcached"
            11311 -> "هماهنگ‌کننده نود اصلی سیستم‌عامل رباتیک ROS"
            27017 -> "پایگاه‌داده اسنادی NoSQL مانگودی‌بی MongoDB"
            47808 -> "شبکه اتوماسیون و کنترل هوشمند ساختمان BACnet/IP"
            50051 -> "اندپوینت میکروسرویس پرسرعت بر بستر HTTP/2 gRPC"
            51820 -> "کانال رمزنگاری‌شده UDP شبکه خصوصی WireGuard VPN"
            else -> {
                if (!rawService.isNullOrBlank() && rawService != "unknown") {
                    "سرویس فعال $rawService"
                } else {
                    "سرویس فعال شبکه"
                }
            }
        }
    } else if (lang == "ru") {
        return when (port) {
            21 -> "Незашифрованный протокол передачи файлов"
            22 -> "Защищенный удаленный терминал и передача файлов"
            23 -> "Текстовый удаленный терминал без шифрования (небезопасно)"
            25 -> "Почтовый релей протокола SMTP"
            53 -> "Преобразователь и сервер доменных имен DNS"
            67, 68 -> "Протокол динамической настройки узла DHCP"
            69 -> "Простой протокол передачи файлов TFTP"
            80 -> "Стандартный веб-протокол передачи гипертекста HTTP"
            102 -> "Промышленный контроллер Siemens Step7 S7comm"
            110 -> "Протокол получения электронной почты POP3"
            123 -> "Синхронизация системного времени по протоколу NTP"
            135 -> "Служба сопоставления конечных точек Microsoft Windows RPC"
            137, 138, 139 -> "Разрешение имен и сеансовый транспорт NetBIOS"
            143 -> "Протокол доступа к электронной почте IMAP"
            161, 162 -> "Протокол сетевого управления и мониторинга SNMP"
            389 -> "Легковесный протокол доступа к каталогам LDAP"
            443 -> "Защищенный веб-сервер с шифрованием TLS/SSL"
            445 -> "Общий доступ к файлам Microsoft SMB / Active Directory"
            465 -> "Защищенная отправка почты поверх SSL/TLS"
            502 -> "Промышленный протокол диспетчерского управления Modbus TCP"
            514 -> "Служба сбора системных журналов и событий Syslog"
            515 -> "Сетевой диспетчер печати Line Printer Daemon"
            548 -> "Протокол файлового обмена Apple Filing Protocol"
            587 -> "Отправка почтовых сообщений с поддержкой STARTTLS"
            631 -> "Сетевая служба интернет-печати CUPS IPP"
            636 -> "Защищенный каталог LDAP поверх TLS"
            993 -> "Защищенная почтовая служба IMAP поверх TLS"
            995 -> "Защищенная почтовая служба POP3 поверх TLS"
            1194 -> "Демон виртуальной частной сети OpenVPN"
            1433 -> "Ядро СУБД Microsoft SQL Server"
            1521 -> "Сетевой прослушиватель базы данных Oracle TNS"
            1700 -> "Шлюз передачи пакетов беспроводной сети LoRaWAN"
            1723 -> "Туннельный протокол точка-точка PPTP"
            1812, 1813 -> "Служба аутентификации и учета пользователей RADIUS"
            1883 -> "Брокер телеметрии IoT протокола MQTT без шифрования"
            1884 -> "Мостовой слушатель протокола MQTT для сетей датчиков"
            1900 -> "Протокол простого обнаружения служб SSDP / UPnP"
            2049 -> "Сетевая файловая система Unix Network File System"
            2181 -> "Служба распределенной координации Apache ZooKeeper"
            2375, 2376 -> "API управления контейнерным движком Docker"
            2379, 2380 -> "Распределенное хранилище ключ-значение etcd"
            3000 -> "Дашборд метрик Grafana или интерфейс веб-приложения"
            3306 -> "Реляционная СУБД MariaDB / MySQL"
            3389 -> "Терминал удаленного рабочего стола Windows RDP"
            4222 -> "Высокопроизводительная система сообщений NATS"
            4840 -> "Промышленный сервер архитектуры OPC Unified Architecture"
            5000 -> "Веб-фреймворк или медиасервер UPnP"
            5060, 5061 -> "Протокол установления сеансов IP-телефонии SIP"
            5432 -> "Реляционная СУБД PostgreSQL"
            5555 -> "Слушатель отладочного моста Android Debug Bridge"
            5672 -> "Брокер очередей сообщений протокола AMQP (RabbitMQ)"
            5683, 5684 -> "Протокол ограниченного применения для датчиков CoAP"
            5900 -> "Удаленный графический рабочий стол VNC"
            6379 -> "Хранилище данных в оперативной памяти Redis"
            6443 -> "API плоскости управления кластером Kubernetes"
            8000 -> "Альтернативный веб-сервер или сервер приложений HTTP"
            8080 -> "Распространенный прокси / альтернативный HTTP"
            8081, 8088 -> "Альтернативный порт веб-приложения HTTP"
            8123 -> "HTTP-порт аналитической колоночной СУБД ClickHouse"
            8200 -> "Система управления секретами HashiCorp Vault"
            8443 -> "Защищенный альтернативный веб-сервис HTTPS"
            8500 -> "Реестр обнаружения сервисов и хранилище KV Consul"
            8883 -> "Защищенная телеметрия IoT MQTT поверх TLS"
            8888, 9090 -> "Веб-консоль управления или сборщик метрик Prometheus"
            9000 -> "Сервер анализа кода SonarQube или объектное хранилище MinIO S3"
            9042 -> "Сетевой транспорт CQL распределенной СУБД Cassandra"
            9092 -> "Брокер потоковой передачи событий Apache Kafka"
            9100 -> "Прямой сетевой канал печати HP JetDirect"
            9200, 9300 -> "Поисково-аналитическая система Elasticsearch"
            10250 -> "API агента узла кластера Kubernetes Kubelet"
            11211 -> "Высокоскоростная система кэширования Memcached"
            11311 -> "Координатор главного узла Robot Operating System"
            27017 -> "Документоориентированная СУБД MongoDB NoSQL"
            47808 -> "Сетевой протокол автоматизации зданий BACnet/IP"
            50051 -> "Высокоскоростная микрослужба на базе HTTP/2 gRPC"
            51820 -> "Защищенный канал UDP виртуальной сети WireGuard VPN"
            else -> {
                if (!rawService.isNullOrBlank() && rawService != "unknown") {
                    "Активная служба $rawService"
                } else {
                    "Активная сетевая служба"
                }
            }
        }
    }
    return when (port) {
        21 -> "Unencrypted file transfer protocol"
        22 -> "Encrypted remote terminal and file transfer"
        23 -> "Plaintext remote terminal session (insecure)"
        25 -> "Simple Mail Transfer Protocol relay"
        53 -> "Domain Name System resolver / nameserver"
        67, 68 -> "Dynamic Host Configuration Protocol"
        69 -> "Trivial File Transfer Protocol"
        80 -> "Standard HyperText Transfer Protocol"
        102 -> "Siemens Step7 industrial programmable logic controller"
        110 -> "Post Office Protocol version 3"
        123 -> "Network Time Protocol clock synchronization"
        135 -> "Microsoft Windows RPC endpoint locator"
        137, 138, 139 -> "NetBIOS name resolution and session transport"
        143 -> "Internet Message Access Protocol"
        161, 162 -> "Simple Network Management Protocol"
        389 -> "Lightweight Directory Access Protocol"
        443 -> "TLS/SSL encrypted web service"
        445 -> "Microsoft SMB file sharing / Windows domain"
        465 -> "Secure SMTP over SSL/TLS wrapper"
        502 -> "Modbus TCP industrial supervisory control"
        514 -> "Syslog system event logging listener"
        515 -> "Line Printer Daemon network print spooler"
        548 -> "Apple Filing Protocol macOS file sharing"
        587 -> "Mail message submission with STARTTLS"
        631 -> "Internet Printing Protocol CUPS service"
        636 -> "Secure LDAP over TLS"
        993 -> "Secure IMAP over TLS"
        995 -> "Secure POP3 over TLS"
        1194 -> "OpenVPN virtual private network daemon"
        1433 -> "Microsoft SQL Server database engine"
        1521 -> "Oracle TNS database listener"
        1700 -> "Semtech LoRaWAN packet forwarder interface"
        1723 -> "Point-to-Point Tunneling Protocol"
        1812, 1813 -> "Remote Authentication Dial-In User Service"
        1883 -> "MQTT telemetry transport broker (unencrypted)"
        1884 -> "MQTT for Sensor Networks bridge listener"
        1900 -> "Simple Service Discovery Protocol"
        2049 -> "Network File System Unix sharing"
        2181 -> "Apache ZooKeeper distributed coordination service"
        2375, 2376 -> "Docker container engine control API"
        2379, 2380 -> "etcd distributed consensus key-value datastore"
        3000 -> "Grafana metrics dashboard or web application frontend"
        3306 -> "MariaDB / relational SQL database"
        3389 -> "Windows Remote Desktop Protocol terminal"
        4222 -> "High-performance NATS publish-subscribe messaging"
        4840 -> "OPC Unified Architecture industrial server"
        5000 -> "Web framework or UPnP media streaming"
        5060, 5061 -> "Session Initiation Protocol VoIP telephony"
        5432 -> "PostgreSQL advanced relational database"
        5555 -> "Android Debug Bridge network listener"
        5672 -> "Advanced Message Queuing Protocol broker"
        5683, 5684 -> "Constrained Application Protocol sensor telemetry"
        5900 -> "Virtual Network Computing remote desktop"
        6379 -> "Redis in-memory key-value data store"
        6443 -> "Kubernetes cluster control plane API"
        8000 -> "Alternate HTTP web / application server"
        8080 -> "Common HTTP alternate / proxy listener"
        8081, 8088 -> "Alternate HTTP web application port"
        8123 -> "ClickHouse columnar analytical database HTTP port"
        8200 -> "HashiCorp Vault secret management"
        8443 -> "Secure HTTPS alternate web service"
        8500 -> "Consul cluster discovery and KV store"
        8883 -> "Secure MQTT IoT telemetry over TLS"
        8888, 9090 -> "Alternate web console or Prometheus"
        9000 -> "SonarQube code quality server or MinIO high-performance S3 object storage"
        9042 -> "Apache Cassandra distributed CQL native transport"
        9092 -> "Apache Kafka event streaming broker"
        9100 -> "HP JetDirect raw network print channel"
        9200, 9300 -> "Elasticsearch RESTful search and transport"
        10250 -> "Kubernetes Kubelet node agent API"
        11211 -> "Memcached high-speed memory object cache"
        11311 -> "Robot Operating System master node coordinator"
        27017 -> "MongoDB NoSQL document database"
        47808 -> "BACnet/IP building automation network"
        50051 -> "High-speed HTTP/2 gRPC microservice endpoint"
        51820 -> "High-speed encrypted WireGuard VPN UDP channel"
        else -> {
            if (!rawService.isNullOrBlank() && rawService != "unknown") {
                "Active $rawService service"
            } else {
                "Active Network Service"
            }
        }
    }
}

// --- Dashboard Implementation ---

@Composable
fun DashboardScreen(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsState()
    val appSettings = LocalAppSettings.current
    val accent = LocalAccentColor.current
    val lang = appSettings.language

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        
        when {
            width >= 1260.dp -> LargeDesktopDashboard(state, viewModel, accent, lang)
            width >= 640.dp -> DesktopDashboard(state, viewModel, accent, lang)
            else -> MobileDashboard(state, viewModel, accent, lang)
        }
    }
}

@Composable
fun LargeDesktopDashboard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. FULL-WIDTH COMMAND CENTER HUD ---
        DashboardIpInput(
            state = state,
            accent = accent,
            lang = lang,
            onIpChange = { viewModel.onIpChange(it) },
            onStartScan = { viewModel.startScan() },
            onStopScan = { viewModel.stopScan() }
        )

        state.error?.let {
            Surface(
                color = DangerNeon.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    LocalizedStrings.get(it, lang), color = DangerNeon, modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // --- 2. BALANCED 3-COLUMN BENTO GRID ---
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Col 1: Visualizer Radar & Live Engine Activity
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecurityVisualizerCard(
                    state,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    accent = accent,
                    lang = lang
                )
                EngineLogsCard(
                    state,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    accent = accent,
                    lang = lang,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }

            // Col 2: Concurrency & Engine Parameters
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdvancedParametersCard(state, viewModel, accent, lang)
                EngineConfigurationCard(state, viewModel, accent, lang)
            }

            // Col 3: Live Discovered Services & Banners
            Column(
                modifier = Modifier.weight(1.15f).fillMaxHeight()
            ) {
                ActiveServicesCard(
                    state,
                    modifier = Modifier.fillMaxSize(),
                    accent = accent,
                    lang = lang
                )
            }
        }
    }
}

@Composable
fun DesktopDashboard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    var leftSubTab by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. FULL-WIDTH COMMAND CENTER HUD ---
        DashboardIpInput(
            state = state,
            accent = accent,
            lang = lang,
            onIpChange = { viewModel.onIpChange(it) },
            onStartScan = { viewModel.startScan() },
            onStopScan = { viewModel.stopScan() }
        )

        state.error?.let {
            Text(LocalizedStrings.get(it, lang), color = DangerNeon, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
        }

        // --- 2. BALANCED 2-COLUMN GRID ---
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Column (weight 1f): Visualizer & Tabbed Logs/Config
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SecurityVisualizerCard(
                    state,
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                    accent = accent,
                    lang = lang
                )
                
                CyberSegmentedControl(
                    items = listOf(
                        LocalizedStrings.get("logs_tab", lang) to Icons.Default.Terminal,
                        LocalizedStrings.get("engine_config", lang) to Icons.Default.Tune
                    ),
                    selectedIndex = leftSubTab,
                    onIndexSelected = { leftSubTab = it },
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedContent(
                    targetState = leftSubTab,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { tab ->
                    if (tab == 0) {
                        EngineLogsCard(
                            state,
                            modifier = Modifier.fillMaxSize(),
                            accent = accent,
                            lang = lang,
                            onClearLogs = { viewModel.clearLogs() }
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        val isRtl = lang == "fa"
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .touchDragScroll(scrollState, isVertical = true)
                                    .verticalScroll(scrollState)
                                    .absolutePadding(
                                        right = if (isRtl) 8.dp else 0.dp,
                                        left = if (!isRtl) 8.dp else 0.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AdvancedParametersCard(state, viewModel, accent = accent, lang = lang)
                                EngineConfigurationCard(state, viewModel, accent = accent, lang = lang)
                            }
                            PortXScrollStateVerticalScrollbar(
                                scrollState = scrollState,
                                modifier = Modifier.align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft).fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // Right Column (weight 1.25f): Discovered Services & CVE Advisories (Full Height)
            Column(
                modifier = Modifier.weight(1.25f).fillMaxHeight()
            ) {
                ActiveServicesCard(
                    state,
                    modifier = Modifier.fillMaxSize(),
                    accent = accent,
                    lang = lang
                )
            }
        }
    }
}

@Composable
fun MobileDashboard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    var showSettings by remember { mutableStateOf(false) }
    var selectedMobileTab by remember { mutableStateOf(0) }
    var isRadarCollapsed by remember { mutableStateOf(false) }
    val mobileListState = rememberLazyListState()

    val result = state.result
    var selectedPortForDetail by remember { mutableStateOf<DisplayPort?>(null) }
    var portSearchQuery by remember { mutableStateOf("") }
    var isPortSearchVisible by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showExportModal by remember { mutableStateOf(false) }

    val allPortsToShow = remember(result, accent, lang) {
        result?.openPorts?.distinct()?.sorted()?.map { portNumber ->
            val banner = (result.portBanners[portNumber] ?: "").trim()
            val rawService = (result.portServices[portNumber] ?: "").trim()
            val title = getServiceTitle(portNumber, rawService.ifEmpty { null }, lang)
            val description = if (banner.isNotEmpty()) banner else getServiceDescription(portNumber, rawService.ifEmpty { null }, lang)
            DisplayPort(portNumber, title, description, getPortColor(portNumber, accent))
        } ?: emptyList()
    }

    val threatCount = remember(allPortsToShow) { allPortsToShow.count { it.color == DangerNeon } }
    val safeCount = remember(allPortsToShow) { allPortsToShow.count { it.color != DangerNeon } }
    val bannerCount = remember(allPortsToShow, result) {
        allPortsToShow.count { !result?.portBanners?.get(it.number).isNullOrBlank() }
    }
    val identifiedCount = remember(allPortsToShow, result) {
        allPortsToShow.count { isIdentifiedPort(it.number, result?.portServices?.get(it.number), result?.portBanners?.get(it.number)) }
    }
    val genericCount = remember(allPortsToShow, identifiedCount) {
        allPortsToShow.size - identifiedCount
    }
    val systemPortsCount = remember(allPortsToShow) { allPortsToShow.count { it.number in 1..1023 } }
    val userPortsCount = remember(allPortsToShow) { allPortsToShow.count { it.number >= 1024 } }
    val webCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(80, 443, 8000, 8008, 8080, 8081, 8088, 8443, 8888, 9000, 9090, 3000, 5000) } }
    val dbCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(1433, 1521, 3306, 5432, 6379, 27017, 9200, 11211) } }
    val remoteCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(21, 22, 23, 3389, 5900, 5985, 5986) } }
    val industrialCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(102, 502, 1883, 4840, 47808, 5683) } }
    val infraCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(53, 67, 68, 123, 161, 389, 636) } }

    val filteredPorts = remember(allPortsToShow, portSearchQuery, selectedCategoryFilter, result) {
        allPortsToShow.filter { port ->
            val banner = result?.portBanners?.get(port.number)?.trim() ?: ""
            val service = result?.portServices?.get(port.number)?.trim() ?: ""
            val isIdent = isIdentifiedPort(port.number, service, banner)

            val matchesCategory = when (selectedCategoryFilter) {
                "THREATS" -> port.color == DangerNeon
                "SAFE" -> port.color != DangerNeon
                "IDENTIFIED" -> isIdent
                "GENERIC" -> !isIdent
                "SYSTEM" -> port.number in 1..1023
                "USER" -> port.number >= 1024
                "BANNER" -> banner.isNotEmpty()
                "WEB" -> port.number in listOf(80, 443, 8000, 8008, 8080, 8081, 8088, 8443, 8888, 9000, 9090, 3000, 5000)
                "DATABASE" -> port.number in listOf(1433, 1521, 3306, 5432, 6379, 27017, 9200, 11211)
                "REMOTE" -> port.number in listOf(21, 22, 23, 3389, 5900, 5985, 5986)
                "INDUSTRIAL" -> port.number in listOf(102, 502, 1883, 4840, 47808, 5683)
                "INFRA" -> port.number in listOf(53, 67, 68, 123, 161, 389, 636)
                else -> true
            }
            if (!matchesCategory) return@filter false

            val rawQ = portSearchQuery.trim()
            if (rawQ.isEmpty()) true
            else {
                val q = rawQ.lowercase()
                val asciiQ = com.mrcoder20.portx.domain.DateFormatter.toAsciiDigits(q)
                val portStr = port.number.toString()
                val portFaStr = com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(portStr)
                val isThreat = port.color == DangerNeon

                portStr.contains(asciiQ) ||
                portFaStr.contains(q) ||
                port.title.lowercase().contains(q) ||
                port.description.lowercase().contains(q) ||
                banner.lowercase().contains(q) ||
                service.lowercase().contains(q) ||
                (isThreat && (q in listOf("خطر", "تهدید", "threat", "danger", "cve"))) ||
                (!isThreat && (q in listOf("امن", "safe", "نرمال", "normal", "ok"))) ||
                (isIdent && (q in listOf("شناسایی", "معروف", "identified", "known"))) ||
                (!isIdent && (q in listOf("عمومی", "نامشخص", "بازتابی", "generic", "unknown", "reflexive"))) ||
                (port.number in 1..1023 && (q in listOf("سیستمی", "سیستم", "system", "privileged"))) ||
                (port.number >= 1024 && (q in listOf("کاربری", "کاربر", "user", "registered")))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = mobileListState,
            modifier = Modifier
                .fillMaxSize()
                .touchDragScroll(mobileListState, isVertical = true)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp)
        ) {
            // 1. IP Target Input HUD
            item(key = "ip_input") {
                DashboardIpInput(
                    state = state,
                    accent = accent,
                    lang = lang,
                    onIpChange = { viewModel.onIpChange(it) },
                    onStartScan = {
                        viewModel.startScan()
                        showSettings = false
                    },
                    onStopScan = { viewModel.stopScan() },
                    showSettingsToggle = true,
                    onSettingsToggle = { showSettings = !showSettings }
                )
            }

            // 2. Expandable Advanced Parameters
            if (showSettings) {
                item(key = "advanced_settings") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdvancedParametersCard(state, viewModel, accent, lang)
                        EngineConfigurationCard(state, viewModel, accent, lang)
                    }
                }
            }

            // 3. Responsive Collapsible / Expandable Radar HUD
            item(key = "radar_hud") {
                AnimatedVisibility(
                    visible = !isRadarCollapsed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SecurityVisualizerCard(
                        state = state,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        smallSize = true,
                        accent = accent,
                        lang = lang,
                        onCollapse = { isRadarCollapsed = true }
                    )
                }

                if (isRadarCollapsed) {
                    Surface(
                        onClick = { isRadarCollapsed = false },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) GlassBackground else GlassLight,
                        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val score = state.result?.securityScore ?: 0
                            val statusColor = when {
                                state.isLoading -> accent
                                state.result != null -> if (score > 80) TertiaryNeon else if (score > 50) (if (isDark) WarningNeon else WarningLight) else (if (isDark) DangerNeon else DangerLight)
                                else -> accent
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(28.dp).background(statusColor.copy(alpha = 0.15f), CircleShape).border(1.dp, statusColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.isLoading) "${state.progress}%" else "$score%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor, fontSize = 9.sp)
                                    )
                                }
                                Text(
                                    text = if (state.isLoading) LocalizedStrings.get("engine_running", lang) else if (state.result != null) "${LocalizedStrings.get("security_score", lang)}: $score%" else LocalizedStrings.get("ready", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isDark) Color.White else Color.Black
                                )
                                if (!state.firewallStatus.isNullOrBlank()) {
                                    Text(
                                        text = "• ${state.firewallStatus}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(Icons.Default.ExpandMore, contentDescription = LocalizedStrings.get("expand", lang), tint = accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 4. Tab Selector
            item(key = "tab_selector") {
                CyberSegmentedControl(
                    items = listOf(
                        LocalizedStrings.get("services_tab", lang) to Icons.Default.Hub,
                        LocalizedStrings.get("logs_tab", lang) to Icons.Default.Terminal
                    ),
                    selectedIndex = selectedMobileTab,
                    onIndexSelected = { selectedMobileTab = it },
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Scan Error (if any)
            state.error?.let { errorMsg ->
                item(key = "scan_error") {
                    Text(
                        text = LocalizedStrings.get(errorMsg, lang),
                        color = DangerNeon,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // 6. Tab Content Stream
            if (selectedMobileTab == 0) {
                // Control Card: High-Density 2-Row Command Bar (~76dp height)
                item(key = "services_controls_card") {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1: Active Services Title, Counts, Micro OS Fingerprint, and Actions (Search Toggle + Export)
                            if (isPortSearchVisible) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color.White.copy(alpha = 0.05f) else SurfaceInsetLight,
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(6.dp))
                                        BasicTextField(
                                            value = portSearchQuery,
                                            onValueChange = { portSearchQuery = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isDark) Color.White else Color.Black,
                                                fontSize = 11.5.sp,
                                                textDirection = TextDirection.ContentOrRtl
                                            ),
                                            cursorBrush = SolidColor(accent),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (portSearchQuery.isEmpty()) {
                                                        Text(
                                                            LocalizedStrings.get("search_ports_placeholder", lang),
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                            color = if (isDark) TextMuted else TextMutedLight,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        if (portSearchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { portSearchQuery = "" },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(13.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                isPortSearchVisible = false
                                                portSearchQuery = ""
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f, fill = false),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            LocalizedStrings.get("active_services", lang),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = if (isDark) Color.White else Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp
                                            ),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (allPortsToShow.isNotEmpty()) {
                                            val countRaw = if (filteredPorts.size != allPortsToShow.size) "${filteredPorts.size}/${allPortsToShow.size}" else "${allPortsToShow.size}"
                                            val displayCount = if (lang == "fa") com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(countRaw) else countRaw
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = accent.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    displayCount,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                    color = accent,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Micro OS Fingerprint Badge
                                        if (result != null && (!result.deviceName.isNullOrBlank() || !result.osFingerprint.isNullOrBlank())) {
                                            val osText = result.deviceName ?: result.osFingerprint ?: ""
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDark) Color.White.copy(alpha = 0.06f) else SurfaceInsetLight,
                                                border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(Icons.Default.Devices, null, tint = accent, modifier = Modifier.size(11.dp))
                                                    Text(
                                                        text = osText,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                                                        color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Action Buttons: Search Toggle & Export
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { isPortSearchVisible = true },
                                            modifier = Modifier.size(28.dp).springPress()
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = LocalizedStrings.get("search", lang),
                                                tint = if (portSearchQuery.isNotEmpty()) accent else (if (isDark) Color.White.copy(0.7f) else Color.Black.copy(0.7f)),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        if (result != null) {
                                            IconButton(
                                                onClick = { showExportModal = true },
                                                modifier = Modifier.size(28.dp).springPress()
                                            ) {
                                                Icon(
                                                    Icons.Default.FileDownload,
                                                    contentDescription = LocalizedStrings.get("export_report", lang),
                                                    tint = accent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Row 2: Category Filter Chips + Unified Interactive Threat Chip
                            val chipsScrollState = rememberScrollState()
                            LaunchedAutoScrollHint(chipsScrollState)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .horizontalFadingEdges(chipsScrollState, fadeWidth = 14.dp, isRtl = isRtl)
                                    .touchDragScroll(chipsScrollState, isVertical = false, isRtl = isRtl)
                                    .horizontalScroll(chipsScrollState),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_all", lang),
                                    count = allPortsToShow.size,
                                    isSelected = selectedCategoryFilter == "ALL",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = "ALL" }
                                )

                                // Unified Interactive Threat Chip (Replaces bulky 40dp banner)
                                val totalThreatCount = threatCount.coerceAtLeast(state.anomalies.size)
                                if (totalThreatCount > 0) {
                                    val isThreatFilterSelected = selectedCategoryFilter == "THREATS"
                                    Surface(
                                        onClick = { selectedCategoryFilter = if (isThreatFilterSelected) "ALL" else "THREATS" },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isThreatFilterSelected) DangerNeon.copy(alpha = 0.25f) else DangerNeon.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, if (isThreatFilterSelected) DangerNeon else DangerNeon.copy(alpha = 0.45f)),
                                        modifier = Modifier.height(26.dp).springPress(pressedScale = 0.94f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxHeight().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, null, tint = DangerNeon, modifier = Modifier.size(12.dp))
                                            val threatText = if (lang == "fa") "${com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(totalThreatCount.toString())} تهدید" else "$totalThreatCount Threats"
                                            Text(
                                                threatText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                softWrap = false,
                                                color = DangerNeon
                                            )
                                        }
                                    }
                                }

                                if (safeCount > 0 && threatCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_safe", lang),
                                        count = safeCount,
                                        isSelected = selectedCategoryFilter == "SAFE",
                                        accent = TertiaryNeon,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "SAFE") "ALL" else "SAFE" }
                                    )
                                }
                                if (identifiedCount > 0 && genericCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_identified", lang),
                                        count = identifiedCount,
                                        isSelected = selectedCategoryFilter == "IDENTIFIED",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "IDENTIFIED") "ALL" else "IDENTIFIED" }
                                    )
                                }
                                if (genericCount > 0 && identifiedCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_generic", lang),
                                        count = genericCount,
                                        isSelected = selectedCategoryFilter == "GENERIC",
                                        accent = if (isDark) TextMuted else TextMutedLight,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "GENERIC") "ALL" else "GENERIC" }
                                    )
                                }
                                if (bannerCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_banner", lang),
                                        count = bannerCount,
                                        isSelected = selectedCategoryFilter == "BANNER",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "BANNER") "ALL" else "BANNER" }
                                    )
                                }
                                if (webCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_web", lang),
                                        count = webCount,
                                        isSelected = selectedCategoryFilter == "WEB",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "WEB") "ALL" else "WEB" }
                                    )
                                }
                                if (dbCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_db", lang),
                                        count = dbCount,
                                        isSelected = selectedCategoryFilter == "DATABASE",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "DATABASE") "ALL" else "DATABASE" }
                                    )
                                }
                                if (remoteCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_remote", lang),
                                        count = remoteCount,
                                        isSelected = selectedCategoryFilter == "REMOTE",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "REMOTE") "ALL" else "REMOTE" }
                                    )
                                }
                                if (industrialCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_industrial", lang),
                                        count = industrialCount,
                                        isSelected = selectedCategoryFilter == "INDUSTRIAL",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "INDUSTRIAL") "ALL" else "INDUSTRIAL" }
                                    )
                                }
                                if (infraCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_infra", lang),
                                        count = infraCount,
                                        isSelected = selectedCategoryFilter == "INFRA",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "INFRA") "ALL" else "INFRA" }
                                    )
                                }
                                if (systemPortsCount > 0 && userPortsCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_system_ports", lang),
                                        count = systemPortsCount,
                                        isSelected = selectedCategoryFilter == "SYSTEM",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "SYSTEM") "ALL" else "SYSTEM" }
                                    )
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_registered_ports", lang),
                                        count = userPortsCount,
                                        isSelected = selectedCategoryFilter == "USER",
                                        accent = accent,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "USER") "ALL" else "USER" }
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                    }
                }

                // Discovered Port Items (Lazy-Loaded List with zero clipping)
                if (filteredPorts.isEmpty()) {
                    item(key = "empty_ports_state") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) GlassSurface else Color.White,
                            border = BorderStroke(1.dp, if (isDark) GlassBorder else BorderLight)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (state.isLoading) Icons.Default.HourglassEmpty else Icons.Default.SearchOff,
                                        null,
                                        tint = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (state.isLoading) LocalizedStrings.get("scanning_network", lang)
                                        else if (portSearchQuery.isNotEmpty() || selectedCategoryFilter != "ALL") LocalizedStrings.get("no_ports_found", lang)
                                        else LocalizedStrings.get("no_services", lang),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (portSearchQuery.isNotEmpty() || selectedCategoryFilter != "ALL") {
                                        Spacer(Modifier.height(6.dp))
                                        TextButton(onClick = {
                                            portSearchQuery = ""
                                            selectedCategoryFilter = "ALL"
                                        }) {
                                            Text(LocalizedStrings.get("clear_filter", lang), color = accent, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredPorts, key = { it.number }) { port ->
                        PortItem(port = port, onClick = { selectedPortForDetail = port })
                    }
                }
            } else {
                // Logs Tab Content
                item(key = "engine_logs") {
                    EngineLogsCard(
                        state = state,
                        modifier = Modifier.fillMaxWidth().height(480.dp),
                        accent = accent,
                        lang = lang,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
            }
        }

        // Automatic RTL / LTR Scrollbar
        val canScroll = mobileListState.canScrollForward || mobileListState.canScrollBackward
        if (canScroll) {
            PortXVerticalScrollbar(
                listState = mobileListState,
                modifier = Modifier
                    .align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                    .fillMaxHeight()
            )
        }
    }

    if (selectedPortForDetail != null) {
        PortDetailDialog(
            port = selectedPortForDetail!!,
            target = result?.target ?: state.ip.ifBlank { "127.0.0.1" },
            rawBanner = result?.portBanners?.get(selectedPortForDetail!!.number),
            rawService = result?.portServices?.get(selectedPortForDetail!!.number),
            accent = accent,
            lang = lang,
            isMobile = true,
            onDismiss = { selectedPortForDetail = null }
        )
    }

    if (showExportModal && result != null) {
        ReportExportDialog(
            scan = result,
            accent = accent,
            lang = lang,
            isDark = isDark,
            isMobile = true,
            onDismiss = { showExportModal = false }
        )
    }
}

@Composable
fun DashboardIpInput(
    state: ScanUIState, 
    accent: Color,
    lang: String,
    onIpChange: (String) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    showSettingsToggle: Boolean = false,
    onSettingsToggle: () -> Unit = {}
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    var isInputFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDark) Modifier else Modifier.shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = Color(0x180F172A),
                    ambientColor = Color(0x0E0F172A)
                )
            ),
        color = if (isDark) GlassBackground else Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else BorderLight)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            TargetScanInputGroup(
                state = state,
                accent = accent,
                lang = lang,
                isDark = isDark,
                showSettingsToggle = showSettingsToggle,
                onSettingsToggle = onSettingsToggle,
                onIpChange = onIpChange,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
                isFocused = isInputFocused,
                onFocusChanged = { isInputFocused = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TargetScanInputGroup(
    state: ScanUIState,
    accent: Color,
    lang: String,
    isDark: Boolean,
    showSettingsToggle: Boolean,
    onSettingsToggle: () -> Unit,
    onIpChange: (String) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    isFocused: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accent.copy(alpha = 0.85f) else (if (isDark) GlassBorder.copy(alpha = 0.6f) else BorderLight),
        animationSpec = tween(durationMillis = 200),
        label = "TargetInputBorderColor"
    )

    Row(
        modifier = modifier.height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color.Black.copy(alpha = 0.35f) else SurfaceInsetLight,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSettingsToggle) {
                    IconButton(
                        onClick = onSettingsToggle,
                        modifier = Modifier
                            .size(28.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isFocused) accent else accent.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                BasicTextField(
                    value = state.ip,
                    onValueChange = onIpChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = if (isDark) Color.White else Color.Black,
                        textDirection = TextDirection.Ltr
                    ),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onGo = {
                            if (state.isLoading) onStopScan() else onStartScan()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (state.ip.isEmpty()) {
                                Text(
                                    LocalizedStrings.get("target", lang),
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (state.ip.isNotEmpty()) {
                    val ipTrimmed = state.ip.trim()
                    val isIpv4 = ipTrimmed.split(".").let { parts -> parts.size == 4 && parts.all { p -> p.toIntOrNull() in 0..255 } }
                    val isLocal = ipTrimmed == "127.0.0.1" || ipTrimmed.equals("localhost", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = (if (isLocal) TertiaryNeon else accent).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, (if (isLocal) TertiaryNeon else accent).copy(alpha = 0.4f)),
                        modifier = Modifier.height(20.dp).padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isLocal) "LOCAL" else if (isIpv4) "IPv4" else "HOST",
                                color = if (isLocal) TertiaryNeon else accent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = { onIpChange("") },
                        modifier = Modifier
                            .size(24.dp)
                            .springPress()
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = LocalizedStrings.get("clear_filter", lang), tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        val infiniteTransition = rememberInfiniteTransition()
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
        )

        Button(
            onClick = { if (state.isLoading) onStopScan() else onStartScan() },
            colors = ButtonDefaults.buttonColors(containerColor = if (state.isLoading) DangerNeon.copy(alpha = pulseAlpha) else accent),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(40.dp).defaultMinSize(minWidth = 64.dp).springPress(pressedScale = 0.94f).pointerHoverIcon(PointerIcon.Hand),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            Icon(if (state.isLoading) Icons.Default.Stop else Icons.Default.FlashOn, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (state.isLoading) LocalizedStrings.get("stop", lang) else LocalizedStrings.get("scan", lang), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 11.5.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedParametersCard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    val headerLetterSpacing = if (isRtl) 0.sp else 1.sp
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(LocalizedStrings.get("advanced", lang), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = headerLetterSpacing), color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            // Engine Features in a clean 2x2 symmetrical grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanChip(
                        label = LocalizedStrings.get("banner_grabbing", lang),
                        checked = state.bannerGrabbing,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.toggleBannerGrabbing(it) }

                    ScanChip(
                        label = LocalizedStrings.get("full_port_scan", lang),
                        checked = state.allPorts,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.toggleAllPorts(it) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanChip(
                        label = LocalizedStrings.get("multi_protocol", lang),
                        checked = state.allProtocols,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.toggleAllProtocols(it) }

                    ScanChip(
                        label = LocalizedStrings.get("stealth_mode", lang),
                        checked = state.scanType == "SYN",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.onScanTypeChange(if (it) "SYN" else "TCP") }
                }
            }

            if (!state.allPorts) {
                val currentRange = "${state.startPort}-${state.endPort}"
                Spacer(Modifier.height(16.dp))
                Text(LocalizedStrings.get("port_range", lang), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = headerLetterSpacing), color = if (isDark) TextMuted else TextMutedLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanChip(LocalizedStrings.get("top_20", lang), currentRange == "1-100", accent) {
                        viewModel.setPortRange("1", "100")
                    }
                    ScanChip(LocalizedStrings.get("standard_ports", lang), currentRange == "1-1024", accent) {
                        viewModel.setPortRange("1", "1024")
                    }
                    ScanChip(LocalizedStrings.get("web_ports", lang), currentRange == "80-8443", accent) {
                        viewModel.setPortRange("80", "8443")
                    }
                    ScanChip(LocalizedStrings.get("db_services", lang), currentRange == "1433-27017", accent) {
                        viewModel.setPortRange("1433", "27017")
                    }
                    ScanChip(LocalizedStrings.get("remote_iot", lang), currentRange == "22-1883", accent) {
                        viewModel.setPortRange("22", "1883")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.startPort,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 5) viewModel.onStartPortChange(it) },
                        label = { Text(LocalizedStrings.get("start_port", lang), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = if (isDark) GlassBorder else GlassBorderLight,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            textDirection = TextDirection.Ltr
                        )
                    )
                    OutlinedTextField(
                        value = state.endPort,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 5) viewModel.onEndPortChange(it) },
                        label = { Text(LocalizedStrings.get("end_port", lang), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = if (isDark) GlassBorder else GlassBorderLight,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            textDirection = TextDirection.Ltr
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EngineConfigurationCard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    val headerLetterSpacing = if (isRtl) 0.sp else 1.sp
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(LocalizedStrings.get("engine_configuration", lang), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = headerLetterSpacing), color = if (isDark) TextMuted else TextMutedLight, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(LocalizedStrings.get("parallel_threads", lang), color = if (isDark) Color.White else Color.Black, style = MaterialTheme.typography.bodySmall)
                    Text("${state.concurrentScans} ${LocalizedStrings.get("connections", lang)}", color = accent, style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = state.concurrentScans.toFloat(),
                    onValueChange = { viewModel.onConcurrentScansChange(it.toInt()) },
                    valueRange = 10f..2000f,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(
                        thumbColor = accent, 
                        activeTrackColor = accent, 
                        inactiveTrackColor = if (isDark) GlassBorder else GlassBorderLight
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(LocalizedStrings.get("socket_timeout", lang), color = if (isDark) Color.White else Color.Black, style = MaterialTheme.typography.bodySmall)
                    Text("${state.timeout} ${LocalizedStrings.get("ms", lang)}", color = accent, style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = state.timeout.toFloat(),
                    onValueChange = { viewModel.onTimeoutChange(it.toInt()) },
                    valueRange = 100f..3000f,
                    steps = 28,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(
                        thumbColor = accent, 
                        activeTrackColor = accent, 
                        inactiveTrackColor = if (isDark) GlassBorder else GlassBorderLight
                    )
                )
            }
        }
    }
}

@Composable
fun SecurityVisualizerCard(
    state: ScanUIState, 
    modifier: Modifier = Modifier, 
    smallSize: Boolean = false, 
    accent: Color, 
    lang: String,
    onCollapse: (() -> Unit)? = null
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    GlassCard(
        modifier = modifier, 
        contentPadding = if (smallSize) PaddingValues(horizontal = 10.dp, vertical = 6.dp) else PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BoxWithConstraints(
                contentAlignment = Alignment.Center, 
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                if (onCollapse != null) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
                    ) {
                        Icon(Icons.Default.ExpandLess, contentDescription = LocalizedStrings.get("collapse", lang), tint = accent, modifier = Modifier.size(18.dp))
                    }
                }

                // Dynamically measure available dimensions to guarantee a pure 1:1 circular ratio without distortion
                val availableD = minOf(maxWidth, maxHeight)
                val isCompact = availableD < 165.dp || smallSize
                val computedGaugeSize = availableD.coerceAtLeast(60.dp)

                val displayProgress = if (state.isLoading) state.progress / 100f else {
                    val result = state.result
                    if (result != null) result.securityScore / 100f else 0f
                }
                val statusColor = when {
                    state.isLoading -> accent
                    state.result != null -> {
                        val score = state.result.securityScore
                        when {
                            score > 80 -> TertiaryNeon
                            score > 50 -> if (isDark) WarningNeon else WarningLight
                            else -> if (isDark) DangerNeon else DangerLight
                        }
                    }
                    else -> (if (isDark) Color.White else Color.Black).copy(alpha = 0.2f)
                }

                AdvancedLiquidGauge(
                    progress = displayProgress, 
                    isLoading = state.isLoading, 
                    color = statusColor, 
                    gaugeSize = computedGaugeSize
                )

                val isRtl = lang == "fa"
                val statusLetterSpacing = if (isRtl) 0.sp else (if (isCompact) 0.5.sp else 1.5.sp)

                // Smoothly scaled center metrics
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (state.isLoading) {
                        Text(
                            "${state.progress}%", 
                            style = when {
                                computedGaugeSize < 120.dp -> MaterialTheme.typography.titleMedium
                                computedGaugeSize < 165.dp -> MaterialTheme.typography.titleLarge
                                computedGaugeSize < 205.dp -> MaterialTheme.typography.headlineMedium
                                else -> MaterialTheme.typography.displayMedium
                            }.copy(
                                fontWeight = FontWeight.Black, 
                                color = if (isDark) Color.White else Color.Black, 
                                shadow = Shadow(color = statusColor, blurRadius = 25f)
                            )
                        )
                        Text(
                            LocalizedStrings.get("engine_running", lang), 
                            style = (if (isCompact) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelMedium)
                                .copy(color = statusColor, letterSpacing = statusLetterSpacing),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    } else {
                        val result = state.result
                        if (result != null) {
                            Text(
                                "${result.securityScore}%", 
                                style = when {
                                    computedGaugeSize < 120.dp -> MaterialTheme.typography.titleMedium
                                    computedGaugeSize < 165.dp -> MaterialTheme.typography.titleLarge
                                    computedGaugeSize < 205.dp -> MaterialTheme.typography.headlineMedium
                                    else -> MaterialTheme.typography.displayMedium
                                }.copy(
                                    fontWeight = FontWeight.Black, 
                                    color = if (isDark) Color.White else Color.Black, 
                                    shadow = Shadow(color = statusColor, blurRadius = 25f)
                                )
                            )
                            Text(
                                LocalizedStrings.get("security_score", lang), 
                                style = (if (isCompact) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp) else MaterialTheme.typography.labelMedium)
                                    .copy(color = statusColor, letterSpacing = statusLetterSpacing),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                Icons.Default.Radar, 
                                null, 
                                tint = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f), 
                                modifier = Modifier.size(if (isCompact) 26.dp else 42.dp)
                            )
                            Text(
                                LocalizedStrings.get("ready", lang), 
                                style = (if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium)
                                    .copy(color = if (isDark) TextMuted else TextMutedLight, letterSpacing = statusLetterSpacing),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom Profile / Perimeter Status Container (clean, concise, and localized)
            if (!state.firewallStatus.isNullOrBlank()) {
                Spacer(Modifier.height(if (smallSize) 4.dp else 6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                    border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = if (smallSize) 4.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(if (smallSize) 13.dp else 15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        val localizedFirewall = when {
                            state.firewallStatus.contains("Open Perimeter") -> LocalizedStrings.get("firewall_open", lang)
                            state.firewallStatus.contains("Hardened Perimeter") -> LocalizedStrings.get("firewall_hardened", lang)
                            state.firewallStatus.contains("Standard") -> LocalizedStrings.get("firewall_standard", lang)
                            state.firewallStatus.contains("Firewall") -> LocalizedStrings.get("firewall_filtered", lang)
                            else -> state.firewallStatus
                        }
                        Text(
                            text = localizedFirewall,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = if (smallSize) 10.sp else 11.sp),
                            color = if (isDark) TextMuted else TextMutedLight,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EngineLogsCard(
    state: ScanUIState, 
    modifier: Modifier = Modifier, 
    accent: Color, 
    lang: String = "en",
    onClearLogs: (() -> Unit)? = null
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    val logListState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var isLogsCopied by remember { mutableStateOf(false) }
    var isClearingLogs by remember { mutableStateOf(false) }

    LaunchedEffect(state.logs.size) { if (state.logs.isNotEmpty()) logListState.animateScrollToItem(state.logs.size - 1) }
    LaunchedEffect(isLogsCopied) {
        if (isLogsCopied) {
            kotlinx.coroutines.delay(2000)
            isLogsCopied = false
        }
    }

    GlassCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        LocalizedStrings.get("live_engine_logs", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.06f) else SurfaceInsetLight,
                        border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)
                    ) {
                        val countText = if (lang == "fa") com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(state.logs.size.toString()) else state.logs.size.toString()
                        Text(
                            "$countText ${LocalizedStrings.get("logs_count", lang)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (state.logs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(state.logs.joinToString("\n")))
                                isLogsCopied = true
                            },
                            modifier = Modifier.size(28.dp).springPress()
                        ) {
                            Icon(
                                if (isLogsCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = LocalizedStrings.get("copy_all_logs", lang),
                                tint = if (isLogsCopied) TertiaryNeon else (if (isDark) TextMuted else TextMutedLight),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (onClearLogs != null && state.logs.isNotEmpty()) {
                        IconButton(
                            onClick = { isClearingLogs = true },
                            modifier = Modifier.size(28.dp).springPress(pressedScale = 0.90f)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = LocalizedStrings.get("clear_logs", lang),
                                tint = DangerNeon.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            val logTerminalPalette = remember(isDark, accent) {
                if (isDark) {
                    listOf(
                        Color(0xFFCBD5E1), // Fine silver ash
                        Color(0xFF94A3B8), // Terminal slate dust
                        Color(0xFFE2E8F0), // Titanium ash
                        Color(0xFFF1F5F9), // Terminal text white
                        Color.White,       // Monospace code white
                        Color(0xFF58A6FF), // Cyan terminal log info
                        Color(0xFF3FB950), // Green terminal log ok
                        Color(0xFFF85149), // Red terminal log error
                        accent,            // Cyber badge accent
                        Color(0xFFFF7043), // Hot burning ember
                        Color(0xFFFFCA28)  // Incandescent gold spark
                    )
                } else {
                    listOf(
                        Color(0xFF475569),
                        Color(0xFF1E293B),
                        Color(0xFF64748B),
                        Color(0xFF334155),
                        Color(0xFF0284C7),
                        Color(0xFF16A34A),
                        Color(0xFFDC2626),
                        accent,
                        Color(0xFFFF5722)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            DisintegrationContainer(
                isDisintegrating = isClearingLogs,
                accent = DangerNeon,
                materialColors = logTerminalPalette,
                particleCount = 2200,
                collapseHeight = false,
                durationMs = 1800,
                onDisintegrated = {
                    onClearLogs?.invoke()
                    isClearingLogs = false
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color.Black.copy(alpha = 0.25f) else SurfaceInsetLight, RoundedCornerShape(10.dp))
                        .border(1.dp, if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (state.logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentAlignment = if (isRtl) Alignment.TopEnd else Alignment.TopStart
                        ) {
                            Text(
                                LocalizedStrings.get("waiting_engine_activity", lang),
                                color = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                    textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
                                )
                            )
                        }
                    } else {
                        val isFa = lang == "fa"
                        CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .touchDragScroll(logListState, isVertical = true)
                                    .padding(horizontal = 12.dp)
                            ) {
                                itemsIndexed(state.logs, key = { index, log -> "${index}_$log" }) { _, log ->
                                    val formattedLog = LocalizedStrings.formatLog(log, lang)
                                    val logColor = when {
                                        log.contains("completed", ignoreCase = true) || log.contains("found", ignoreCase = true) || log.contains("active", ignoreCase = true) -> TertiaryNeon
                                        log.contains("error", ignoreCase = true) || log.contains("failed", ignoreCase = true) || log.contains("refused", ignoreCase = true) -> DangerNeon
                                        log.contains("warning", ignoreCase = true) || log.contains("timeout", ignoreCase = true) -> WarningNeon
                                        log.contains("probing", ignoreCase = true) || log.contains("scan", ignoreCase = true) -> accent
                                        else -> (if (isDark) Color.White else Color.Black).copy(alpha = 0.85f)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            if (isRtl) "‹ " else "› ",
                                            color = accent,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            formattedLog,
                                            color = logColor,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = if (isFa) FontFamily.Default else FontFamily.Monospace,
                                                fontSize = 11.sp
                                            ),
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                            PortXVerticalScrollbar(
                                listState = logListState,
                                modifier = Modifier.align(if (isRtl) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipMini(
    label: String,
    count: Int? = null,
    isSelected: Boolean,
    accent: Color,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val settings = LocalAppSettings.current
    val isDark = settings.theme == "DARK"
    val isFa = settings.language == "fa"
    val chipColor = if (isDanger) DangerNeon else accent
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) chipColor.copy(alpha = 0.18f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
        border = BorderStroke(1.dp, if (isSelected) chipColor else (if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)),
        modifier = Modifier
            .height(28.dp)
            .then(
                if (isDark || isSelected) Modifier else Modifier.shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(8.dp),
                    spotColor = Color(0x100F172A),
                    ambientColor = Color(0x0A0F172A)
                )
            )
            .springPress(pressedScale = 0.94f)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.5.sp
                ),
                maxLines = 1,
                softWrap = false,
                color = if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight)
            )
            if (count != null && count > 0) {
                val countStr = if (isFa) com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(count.toString()) else count.toString()
                Text(
                    countStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    softWrap = false,
                    color = if (isSelected) chipColor else (if (isDark) TextMuted else TextMutedLight)
                )
            }
        }
    }
}

@Composable
fun ActiveServicesCard(state: ScanUIState, modifier: Modifier = Modifier, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val result = state.result
    var selectedPortForDetail by remember { mutableStateOf<DisplayPort?>(null) }
    var portSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var showExportModal by remember { mutableStateOf(false) }

    val allPortsToShow = remember(result, accent, lang) {
        result?.openPorts?.distinct()?.sorted()?.map { portNumber ->
            val banner = (result.portBanners[portNumber] ?: "").trim()
            val rawService = (result.portServices[portNumber] ?: "").trim()
            val title = getServiceTitle(portNumber, rawService.ifEmpty { null }, lang)
            val description = if (banner.isNotEmpty()) banner else getServiceDescription(portNumber, rawService.ifEmpty { null }, lang)
            DisplayPort(portNumber, title, description, getPortColor(portNumber, accent))
        } ?: emptyList()
    }

    val threatCount = remember(allPortsToShow) { allPortsToShow.count { it.color == DangerNeon } }
    val safeCount = remember(allPortsToShow) { allPortsToShow.count { it.color != DangerNeon } }
    val bannerCount = remember(allPortsToShow, result) {
        allPortsToShow.count { !result?.portBanners?.get(it.number).isNullOrBlank() }
    }
    val identifiedCount = remember(allPortsToShow, result) {
        allPortsToShow.count { isIdentifiedPort(it.number, result?.portServices?.get(it.number), result?.portBanners?.get(it.number)) }
    }
    val genericCount = remember(allPortsToShow, identifiedCount) {
        allPortsToShow.size - identifiedCount
    }
    val systemPortsCount = remember(allPortsToShow) { allPortsToShow.count { it.number in 1..1023 } }
    val userPortsCount = remember(allPortsToShow) { allPortsToShow.count { it.number >= 1024 } }
    val webCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(80, 443, 8000, 8008, 8080, 8081, 8088, 8443, 8888, 9000, 9090, 3000, 5000) } }
    val dbCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(1433, 1521, 3306, 5432, 6379, 27017, 9200, 11211) } }
    val remoteCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(21, 22, 23, 3389, 5900, 5985, 5986) } }
    val industrialCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(102, 502, 1883, 4840, 47808, 5683) } }
    val infraCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(53, 67, 68, 123, 161, 389, 636) } }

    val filteredPorts = remember(allPortsToShow, portSearchQuery, selectedCategoryFilter, result) {
        allPortsToShow.filter { port ->
            val banner = result?.portBanners?.get(port.number)?.trim() ?: ""
            val service = result?.portServices?.get(port.number)?.trim() ?: ""
            val isIdent = isIdentifiedPort(port.number, service, banner)

            val matchesCategory = when (selectedCategoryFilter) {
                "THREATS" -> port.color == DangerNeon
                "SAFE" -> port.color != DangerNeon
                "IDENTIFIED" -> isIdent
                "GENERIC" -> !isIdent
                "SYSTEM" -> port.number in 1..1023
                "USER" -> port.number >= 1024
                "BANNER" -> banner.isNotEmpty()
                "WEB" -> port.number in listOf(80, 443, 8000, 8008, 8080, 8081, 8088, 8443, 8888, 9000, 9090, 3000, 5000)
                "DATABASE" -> port.number in listOf(1433, 1521, 3306, 5432, 6379, 27017, 9200, 11211)
                "REMOTE" -> port.number in listOf(21, 22, 23, 3389, 5900, 5985, 5986)
                "INDUSTRIAL" -> port.number in listOf(102, 502, 1883, 4840, 47808, 5683)
                "INFRA" -> port.number in listOf(53, 67, 68, 123, 161, 389, 636)
                else -> true
            }
            if (!matchesCategory) return@filter false

            val rawQ = portSearchQuery.trim()
            if (rawQ.isEmpty()) true
            else {
                val q = rawQ.lowercase()
                val asciiQ = com.mrcoder20.portx.domain.DateFormatter.toAsciiDigits(q)
                val portStr = port.number.toString()
                val portFaStr = com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(portStr)
                val isThreat = port.color == DangerNeon

                portStr.contains(asciiQ) ||
                portFaStr.contains(q) ||
                port.title.lowercase().contains(q) ||
                port.description.lowercase().contains(q) ||
                banner.lowercase().contains(q) ||
                service.lowercase().contains(q) ||
                (isThreat && (q in listOf("خطر", "تهدید", "threat", "danger", "cve"))) ||
                (!isThreat && (q in listOf("امن", "safe", "نرمال", "normal", "ok"))) ||
                (isIdent && (q in listOf("شناسایی", "معروف", "identified", "known"))) ||
                (!isIdent && (q in listOf("عمومی", "نامشخص", "بازتابی", "generic", "unknown", "reflexive"))) ||
                (port.number in 1..1023 && (q in listOf("سیستمی", "سیستم", "system", "privileged"))) ||
                (port.number >= 1024 && (q in listOf("کاربری", "کاربر", "user", "registered")))
            }
        }
    }

    GlassCard(modifier = modifier) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                // Title & Count Pill Badge (Start: Right in RTL / Left in LTR)
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        LocalizedStrings.get("active_services", lang),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (isDark) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (allPortsToShow.isNotEmpty()) {
                        val countRaw = if (filteredPorts.size != allPortsToShow.size) "${filteredPorts.size}/${allPortsToShow.size}" else "${allPortsToShow.size}"
                        val displayCount = if (lang == "fa") com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(countRaw) else countRaw
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                displayCount,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = accent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                }

                // Export Report Button (End: Left in RTL / Right in LTR)
                if (result != null) {
                    Surface(
                        onClick = { showExportModal = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.5f) else BorderLight),
                        modifier = Modifier
                            .then(
                                if (isDark) Modifier else Modifier.shadow(
                                    elevation = 1.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    spotColor = Color(0x100F172A),
                                    ambientColor = Color(0x0A0F172A)
                                )
                            )
                            .springPress(pressedScale = 0.95f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = LocalizedStrings.get("export_report", lang),
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                LocalizedStrings.get("export_report", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (result != null && (!result.deviceName.isNullOrBlank() || !result.osFingerprint.isNullOrBlank())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else SurfaceInsetLight, RoundedCornerShape(8.dp))
                        .border(1.dp, if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Devices, null, tint = accent, modifier = Modifier.size(16.dp))
                    val deviceText = buildString {
                        if (!result.deviceName.isNullOrBlank()) append(result.deviceName)
                        if (!result.osFingerprint.isNullOrBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(result.osFingerprint)
                        }
                    }
                    Text(
                        text = deviceText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            if (state.anomalies.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(DangerNeon.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, DangerNeon.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = DangerNeon, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${state.anomalies.size} ${LocalizedStrings.get("threats_detected", lang)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DangerNeon
                    )
                }
            }

            // --- PORT SEARCH & FILTER CONTROLS ---
            if (allPortsToShow.isNotEmpty()) {
                val hasCategories = threatCount > 0 || safeCount > 0 || bannerCount > 0 || webCount > 0 || dbCount > 0 || remoteCount > 0 || industrialCount > 0 || infraCount > 0
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    // Compact, vertically centered search bar with zero text clipping
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                        border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.4f) else BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = portSearchQuery,
                                onValueChange = { portSearchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color.White else Color.Black,
                                    textDirection = TextDirection.ContentOrRtl
                                ),
                                cursorBrush = SolidColor(accent),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (portSearchQuery.isEmpty()) {
                                            Text(
                                                LocalizedStrings.get("search_ports_placeholder", lang),
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = if (isDark) TextMuted else TextMutedLight,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (portSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { portSearchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isDark) TextMuted else TextMutedLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick category chips
                    if (hasCategories) {
                        val chipsScrollState = rememberScrollState()
                        LaunchedAutoScrollHint(chipsScrollState)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .horizontalFadingEdges(chipsScrollState, fadeWidth = 14.dp, isRtl = isRtl)
                                .touchDragScroll(chipsScrollState, isVertical = false, isRtl = isRtl)
                                .horizontalScroll(chipsScrollState),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChipMini(
                                label = LocalizedStrings.get("filter_all", lang),
                                count = allPortsToShow.size,
                                isSelected = selectedCategoryFilter == "ALL",
                                accent = accent,
                                onClick = { selectedCategoryFilter = "ALL" }
                            )
                            if (threatCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_threats", lang),
                                    count = threatCount,
                                    isSelected = selectedCategoryFilter == "THREATS",
                                    accent = DangerNeon,
                                    isDanger = true,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "THREATS") "ALL" else "THREATS" }
                                )
                            }
                            if (safeCount > 0 && threatCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_safe", lang),
                                    count = safeCount,
                                    isSelected = selectedCategoryFilter == "SAFE",
                                    accent = TertiaryNeon,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "SAFE") "ALL" else "SAFE" }
                                )
                            }
                            if (identifiedCount > 0 && genericCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_identified", lang),
                                    count = identifiedCount,
                                    isSelected = selectedCategoryFilter == "IDENTIFIED",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "IDENTIFIED") "ALL" else "IDENTIFIED" }
                                )
                            }
                            if (genericCount > 0 && identifiedCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_generic", lang),
                                    count = genericCount,
                                    isSelected = selectedCategoryFilter == "GENERIC",
                                    accent = if (isDark) TextMuted else TextMutedLight,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "GENERIC") "ALL" else "GENERIC" }
                                )
                            }
                            if (bannerCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_banner", lang),
                                    count = bannerCount,
                                    isSelected = selectedCategoryFilter == "BANNER",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "BANNER") "ALL" else "BANNER" }
                                )
                            }
                            if (webCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_web", lang),
                                    count = webCount,
                                    isSelected = selectedCategoryFilter == "WEB",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "WEB") "ALL" else "WEB" }
                                )
                            }
                            if (dbCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_db", lang),
                                    count = dbCount,
                                    isSelected = selectedCategoryFilter == "DATABASE",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "DATABASE") "ALL" else "DATABASE" }
                                )
                            }
                            if (remoteCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_remote", lang),
                                    count = remoteCount,
                                    isSelected = selectedCategoryFilter == "REMOTE",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "REMOTE") "ALL" else "REMOTE" }
                                )
                            }
                            if (industrialCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_industrial", lang),
                                    count = industrialCount,
                                    isSelected = selectedCategoryFilter == "INDUSTRIAL",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "INDUSTRIAL") "ALL" else "INDUSTRIAL" }
                                )
                            }
                            if (infraCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_infra", lang),
                                    count = infraCount,
                                    isSelected = selectedCategoryFilter == "INFRA",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "INFRA") "ALL" else "INFRA" }
                                )
                            }
                            if (systemPortsCount > 0 && userPortsCount > 0) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_system_ports", lang),
                                    count = systemPortsCount,
                                    isSelected = selectedCategoryFilter == "SYSTEM",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "SYSTEM") "ALL" else "SYSTEM" }
                                )
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_registered_ports", lang),
                                    count = userPortsCount,
                                    isSelected = selectedCategoryFilter == "USER",
                                    accent = accent,
                                    onClick = { selectedCategoryFilter = if (selectedCategoryFilter == "USER") "ALL" else "USER" }
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }

            // --- PORTS LIST + AUTOMATIC RTL/LTR SCROLLBAR ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val canScroll = listState.canScrollForward || listState.canScrollBackward
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .touchDragScroll(listState, isVertical = true)
                        .absolutePadding(
                            right = if (isRtl && canScroll) 10.dp else 0.dp,
                            left = if (!isRtl && canScroll) 10.dp else 0.dp
                        ),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (filteredPorts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (state.isLoading) Icons.Default.HourglassEmpty else Icons.Default.SearchOff,
                                        null,
                                        tint = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        if (state.isLoading) LocalizedStrings.get("scanning_network", lang)
                                        else if (portSearchQuery.isNotEmpty() || selectedCategoryFilter != "ALL") LocalizedStrings.get("no_ports_found", lang)
                                        else LocalizedStrings.get("no_services", lang),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (portSearchQuery.isNotEmpty() || selectedCategoryFilter != "ALL") {
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(onClick = {
                                            portSearchQuery = ""
                                            selectedCategoryFilter = "ALL"
                                        }) {
                                            Text(LocalizedStrings.get("clear_filter", lang), color = accent, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredPorts, key = { it.number }) { port ->
                            PortItem(port = port, onClick = { selectedPortForDetail = port })
                        }
                    }
                }

                // Automatic Right for RTL (fa/ar), Left for LTR (en/etc.) - only rendered if scrollable
                if (canScroll) {
                    PortXVerticalScrollbar(
                        listState = listState,
                        modifier = Modifier
                            .align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                            .fillMaxHeight()
                    )
                }

                if (canScroll) {
                    Box(modifier = Modifier.fillMaxWidth().height(32.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, (if (isDark) BackgroundDark else BackgroundLight).copy(alpha = 0.5f)))))
                }
            }

            if (selectedPortForDetail != null) {
                PortDetailDialog(
                    port = selectedPortForDetail!!,
                    target = result?.target ?: state.ip.ifBlank { "127.0.0.1" },
                    rawBanner = result?.portBanners?.get(selectedPortForDetail!!.number),
                    rawService = result?.portServices?.get(selectedPortForDetail!!.number),
                    accent = accent,
                    lang = lang,
                    onDismiss = { selectedPortForDetail = null }
                )
            }

            if (showExportModal && result != null) {
                ReportExportDialog(
                    scan = result,
                    accent = accent,
                    lang = lang,
                    isDark = isDark,
                    onDismiss = { showExportModal = false }
                )
            }
        }
    }
}

@Composable
fun ScanChip(label: String, checked: Boolean, accent: Color, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val shadowModifier = if (isDark || checked) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = 1.dp,
            shape = RoundedCornerShape(12.dp),
            ambientColor = Color(0x0C0F172A),
            spotColor = Color(0x100F172A)
        )
    }
    Surface(
        onClick = { onCheckedChange(!checked) }, 
        shape = RoundedCornerShape(12.dp), 
        color = if (checked) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.05f) else Color.White), 
        border = BorderStroke(1.dp, if (checked) accent else (if (isDark) GlassBorder else BorderLight)),
        modifier = modifier
            .then(shadowModifier)
            .springPress(pressedScale = 0.96f)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (checked) accent else (if (isDark) TextMuted else TextMutedLight)))
            Text(
                label, 
                style = MaterialTheme.typography.labelMedium, 
                color = if (checked) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                softWrap = false
            )
        }
    }
}

@Composable
fun AdvancedLiquidGauge(
    progress: Float, 
    isLoading: Boolean, 
    color: Color, 
    gaugeSize: androidx.compose.ui.unit.Dp = 220.dp
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val infiniteTransition = rememberInfiniteTransition()
    
    // Rotating radar beacon is ONLY active when actively loading
    val loadingRotation by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing))
    )
    val dotRotation0 by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val dotRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val dotRotation2 by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f), 
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    // Gauge always anchors at standard 12 o'clock / top position (-90 degrees)
    val baseStartAngle = -90f

    // 1. Root container enforces strict 1:1 square aspect ratio bounded by gaugeSize
    Box(
        modifier = Modifier
            .size(gaugeSize)
            .aspectRatio(1f), 
        contentAlignment = Alignment.Center
    ) {
        // 2. Concentric radar & progress canvas with pure circular geometry
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val d = minOf(size.width, size.height)
            if (d <= 0f) return@Canvas
            
            val strokeWidth = (d * 0.046f).coerceAtLeast(2.5f)
            // Inset radius so glow and stroke caps never clip the bounding box
            val arcRadius = (d - strokeWidth * 2.4f) / 2f
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val trackColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f)
            
            // Step 1: Full 360 baseline track (exact concentric circle)
            drawCircle(
                color = trackColor,
                radius = arcRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            if (isLoading && animatedProgress < 0.05f) {
                // Active indeterminate scanning radar beacon when progress is not yet reported
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.75f to color.copy(alpha = 0.3f),
                        1f to color,
                        center = center
                    ),
                    startAngle = loadingRotation,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else if (animatedProgress > 0.005f) {
                val sweep = 360f * animatedProgress
                
                // Step 2: Ambient neon bloom / glow behind the progress arc (strictly concentric)
                drawArc(
                    color = color.copy(alpha = 0.28f),
                    startAngle = baseStartAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth * 1.55f, cap = StrokeCap.Round)
                )
                
                // Step 3: Crisp foreground progress arc (strictly concentric)
                drawArc(
                    color = color,
                    startAngle = baseStartAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Step 4: Precision illuminated beacon head at the tip of the arc
                if (animatedProgress > 0.015f && animatedProgress < 0.995f) {
                    val angleRad = ((baseStartAngle + sweep) * (kotlin.math.PI / 180.0)).toFloat()
                    val headX = center.x + arcRadius * kotlin.math.cos(angleRad)
                    val headY = center.y + arcRadius * kotlin.math.sin(angleRad)
                    drawCircle(
                        color = color.copy(alpha = 0.5f), 
                        radius = strokeWidth * 0.85f, 
                        center = Offset(headX, headY)
                    )
                    drawCircle(
                        color = Color.White, 
                        radius = strokeWidth * 0.42f, 
                        center = Offset(headX, headY)
                    )
                }
            }
        }

        // Inner core capsule with radial glow (proportional to gaugeSize, strictly concentric)
        val innerSize = gaugeSize * 0.70f
        Box(
            modifier = Modifier
                .size(innerSize)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = if (isDark) 0.12f else 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp, 
                    color = color.copy(alpha = if (isDark) 0.25f else 0.18f), 
                    shape = CircleShape
                ), 
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                ) {
                    val d = minOf(size.width, size.height)
                    val radius = d * 0.42f
                    val dotRadius = (d * 0.018f).coerceAtLeast(1.5f)
                    val dotRotations = listOf(dotRotation0, dotRotation1, dotRotation2)
                    dotRotations.forEachIndexed { index, rot ->
                        val angleRad = (rot * (kotlin.math.PI / 180.0)).toFloat()
                        val x = center.x + radius * kotlin.math.cos(angleRad)
                        val y = center.y + radius * kotlin.math.sin(angleRad)
                        drawCircle(
                            color = color, 
                            radius = dotRadius, 
                            center = Offset(x, y), 
                            alpha = 0.85f - (index * 0.22f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortItem(port: DisplayPort, onClick: () -> Unit = {}) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else BorderLight),
        color = if (isDark) GlassSurface else Color.White,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDark) Modifier else Modifier.shadow(
                    elevation = 1.5.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0x140F172A),
                    ambientColor = Color(0x0C0F172A)
                )
            )
            .springPress(pressedScale = 0.98f, interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(port.color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(1.dp, port.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(port.number.toString(), color = port.color, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), fontSize = if (port.number > 9999) 9.sp else 11.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = port.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = port.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) TextSecondary else TextSecondaryLight),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(20.dp))
        }
    }
}

fun getPortRiskLevel(port: Int, lang: String = "en"): Triple<String, Color, ImageVector> {
    return when (port) {
        21, 23, 102, 135, 137, 138, 139, 445, 502, 1883, 2049, 2181, 2375, 2379, 5555, 10250, 11211 -> {
            val label = when (lang) {
                "fa" -> "بحرانی / پرخطر"
                "ru" -> "КРИТИЧЕСКИЙ / ВЫСОКИЙ РИСК"
                else -> "CRITICAL / HIGH RISK"
            }
            Triple(label, DangerNeon, Icons.Default.Warning)
        }
        22, 1194, 1433, 1521, 3306, 3389, 4840, 51820, 5432, 5900, 6379, 6443, 8123, 8200, 8500, 9042, 9200, 9300, 27017 -> {
            val label = when (lang) {
                "fa" -> "دسترسی ویژه / مدیریتی"
                "ru" -> "ПРИВИЛЕГИРОВАННЫЙ / АДМИНИСТРАТИВНЫЙ"
                else -> "PRIVILEGED / ADMINISTRATIVE"
            }
            Triple(label, SecondaryNeon, Icons.Default.Lock)
        }
        80, 443, 8000, 8080, 8081, 8088, 8443, 8888, 9090, 3000, 5000 -> {
            val label = when (lang) {
                "fa" -> "وب‌سرویس استاندارد"
                "ru" -> "СТАНДАРТНАЯ ВЕБ-СЛУЖБА"
                else -> "STANDARD WEB SERVICE"
            }
            Triple(label, PrimaryNeon, Icons.Default.Language)
        }
        else -> {
            val label = when (lang) {
                "fa" -> "سرویس شبکه استاندارد"
                "ru" -> "СТАНДАРТНАЯ СЕТЕВАЯ СЛУЖБА"
                else -> "STANDARD NETWORK SERVICE"
            }
            Triple(label, TertiaryNeon, Icons.Default.CheckCircle)
        }
    }
}

fun getPortSecurityAdvisory(port: Int, lang: String = "en"): String {
    if (lang == "fa") {
        return when (port) {
            21 -> "پروتکل FTP ناامن است و اطلاعات ورود را به صورت متن خام منتقل می‌کند. به SFTP (پورت ۲۲) یا FTPS مهاجرت کنید و دسترسی خارجی فایروال را مسدود نمایید."
            22 -> "فقط احراز هویت با کلید عمومی SSH را مجاز کنید، ورود مستقیم کاربر root را غیرفعال کرده و از Fail2ban جهت جلوگیری از حملات Brute-force استفاده کنید."
            23 -> "پروتکل Telnet کاملاً منسوخ و فاقد رمزنگاری است. این سرویس را فوراً متوقف کرده و SSH را جایگزین نمایید."
            53 -> "سرویس DNS شناسایی شد. مطمئن شوید Open Recursion غیرفعال باشد تا از حملات تقویت DNS و مسموم‌سازی کش جلوگیری شود."
            80 -> "ترافیک وب HTTP غیررمزنگاری‌شده است. تغییر مسیر خودکار به HTTPS با هدرهای HSTS و گواهینامه‌های معتبر TLS را اجباری کنید."
            135 -> "سرویس Microsoft RPC یک بردار حمله پرخطر برای نفوذ جانبی و افزایش سطح دسترسی در شبکه ویندوزی است. دسترسی این پورت را در فایروال مسدود کنید."
            137, 138, 139 -> "سرویس‌های NetBIOS اطلاعات ساختار شبکه داخلی را افشا می‌کنند. آن‌ها را به VLAN داخلی محدود کرده یا SMBv1 را کاملاً غیرفعال نمایید."
            443 -> "اندپوینت وب امن HTTPS. پیکربندی TLS را بررسی کنید تا نسخه‌های قدیمی TLS 1.0/1.1 غیرفعال باشند و از رمزنگاری‌های مدرن استفاده شود."
            445 -> "اشتراک فایل SMB / اکتیو دایرکتوری، بردار اصلی انتشار باج‌افزارها (مانند WannaCry) است. هرگز نباید در اینترنت عمومی باز باشد."
            1433 -> "پایگاه‌داده Microsoft SQL Server. رمزهای پیچیده اعمال کنید، کاربر 'sa' را غیرفعال کرده و دسترسی را فقط به VPN یا IPهای مجاز محدود سازید."
            1883 -> "بروکر IoT پروتکل MQTT بدون رمزنگاری است. به پورت ۸۸۸۳ با احراز هویت دوطرفه گواهینامه دیجیتال (mTLS) مهاجرت نمایید."
            3306 -> "پورت پایگاه‌داده MySQL/MariaDB در دسترس است. تنها به localhost (127.0.0.1) گوش فرا دهید یا از تونل امن SSH استفاده کنید."
            3389 -> "سرویس ریموت دسکتاپ ویندوز (RDP). هدف دائمی حملات نفوذ باج‌افزاری است. احراز هویت در سطح شبکه (NLA) را فعال کرده و اتصال را به VPN محدود کنید."
            5432 -> "پورت پایگاه‌داده PostgreSQL. دسترسی را در pg_hba.conf به رنج‌های IP معتبر محدود کرده و اتصال اجباری SSL/TLS را فعال کنید."
            6379 -> "پایگاه‌داده حافظه‌محور Redis به طور پیش‌فرض فاقد احراز هویت است. به 127.0.0.1 متصل شوید، requirepass قوی تعیین کرده و دستورات خطرناک را غیرفعال کنید."
            5900 -> "سرویس ریموت دسکتاپ VNC ترافیک صفحه را منتقل می‌کند. رمز عبور قوی قرار دهید، احراز هویت امن را اجباری کرده و از تونل SSH یا VPN استفاده کنید."
            8080, 8443 -> "سرویس وب یا پراکسی جانبی. مسیرهای عیب‌یابی، Swagger و پنل‌های مدیریتی را بررسی کرده و از عدم استفاده از رمزهای پیش‌فرض مطمئن شوید."
            27017 -> "پورت پایگاه‌داده MongoDB باز است. مطمئن شوید احراز هویت (auth = true) فعال و رمزنگاری داده‌ها در حال انتقال (TLS) برقرار باشد."
            else -> "بررسی کنید که آیا این سرویس واقعاً نیاز به دسترسی عمومی دارد یا خیر. اصل حداقل دسترسی و فیلتر دقیق ترافیک ورودی فایروال را اعمال کنید."
        }
    } else if (lang == "ru") {
        return when (port) {
            21 -> "Протокол FTP передает учетные данные в открытом виде. Перейдите на SFTP (порт 22) или FTPS и заблокируйте внешний доступ брандмауэром."
            22 -> "Разрешите аутентификацию только по SSH-ключам, отключите вход для root и используйте Fail2ban для защиты от брутфорс-атак."
            23 -> "Протокол Telnet устарел и не шифрует трафик. Немедленно отключите службу и перейдите на SSH."
            53 -> "Служба DNS. Убедитесь, что открытая рекурсия отключена для предотвращения атак DNS Amplification и отравления кэша."
            80 -> "Незашифрованный HTTP-трафик. Настройте автоматическое перенаправление на HTTPS со строгими заголовками HSTS и сертификатами TLS."
            135 -> "Служба Microsoft RPC — частый вектор для латерального перемещения и повышения привилегий в сетях Windows. Заблокируйте порт на сетевом экране."
            137, 138, 139 -> "Службы NetBIOS раскрывают топологию внутренней сети. Ограничьте их внутренним VLAN или полностью отключите протокол SMBv1."
            443 -> "Защищенный веб-эндпоинт HTTPS. Убедитесь, что устаревшие версии TLS 1.0/1.1 отключены и используются стойкие алгоритмы шифрования."
            445 -> "Общий доступ SMB / Active Directory — основной вектор распространения программ-вымогателей (WannaCry). Никогда не открывайте в глобальный интернет."
            1433 -> "СУБД Microsoft SQL Server. Установите стойкие пароли, отключите учетную запись 'sa' и ограничьте доступ только через VPN или доверенные IP."
            1883 -> "Брокер протокола MQTT IoT без шифрования. Перейдите на порт 8883 с взаимной аутентификацией по сертификатам (mTLS)."
            3306 -> "Порт базы данных MySQL/MariaDB открыт. Настройте привязку только к localhost (127.0.0.1) или используйте безопасный SSH-туннель."
            3389 -> "Удаленный рабочий стол Windows (RDP) — частая цель атак программ-вымогателей. Включите аутентификацию NLA и ограничьте доступ через VPN."
            5432 -> "Порт СУБД PostgreSQL. Ограничьте адреса в pg_hba.conf доверенными подсетями и активируйте обязательное шифрование SSL/TLS."
            6379 -> "Хранилище данных в памяти Redis по умолчанию без аутентификации. Привяжите к 127.0.0.1, задайте надежный requirepass и отключите опасные команды."
            5900 -> "Удаленный доступ VNC передает графический экран. Задайте надежный пароль, используйте безопасную аутентификацию и подключайтесь через SSH или VPN."
            8080, 8443 -> "Дополнительный веб-сервер или прокси. Проверьте отладочные пути, интерфейсы Swagger и панели администратора на отсутствие стандартных паролей."
            27017 -> "Порт СУБД MongoDB открыт. Убедитесь, что обязательная аутентификация (auth = true) активна и включено шифрование данных TLS."
            else -> "Проверьте, действительно ли эта служба требует внешнего доступа. Применяйте принцип минимальных привилегий и строгую фильтрацию входящего трафика."
        }
    }
    return when (port) {
        21 -> "Unencrypted FTP transmits credentials in plaintext. Migrate to SFTP (Port 22) or FTPS (TLS). Block external access at perimeter firewall."
        22 -> "Enforce SSH public key authentication only, disable root login (PermitRootLogin no), and implement rate-limiting or Fail2ban."
        23 -> "Telnet is deprecated and completely unencrypted. Terminate this legacy daemon immediately and replace with SSH."
        53 -> "DNS resolver detected. Ensure open recursion is disabled to prevent DNS amplification and cache poisoning attacks."
        80 -> "Unencrypted HTTP traffic. Enforce HTTPS redirection with HSTS headers and modern TLS certificates."
        135 -> "Microsoft RPC Endpoint Mapper is a high-profile vector for lateral movement and privilege escalation. Block port 135 at the network perimeter."
        137, 138, 139 -> "NetBIOS services expose internal network topology and credentials. Isolate to internal VLAN or disable SMBv1 entirely."
        443 -> "Secure HTTPS web endpoint. Audit TLS configuration to ensure TLS 1.0/1.1 are disabled and strong cipher suites are enforced."
        445 -> "SMB / Active Directory file sharing. Primary vector for ransomware propagation (e.g. WannaCry/EternalBlue). Never expose to the public internet."
        1433 -> "Microsoft SQL Server database. Enforce strong authentication, disable the 'sa' account, and restrict access via VPN or IP whitelist."
        1883 -> "Unencrypted MQTT IoT broker. Migrate to port 8883 with mutual TLS (mTLS) authentication."
        3306 -> "MySQL/MariaDB database port exposed. Bind exclusively to localhost (127.0.0.1) or enforce SSH tunneling."
        3389 -> "Remote Desktop Protocol (RDP). High-risk target for brute-force ransomware attacks. Require VPN connection and enable NLA."
        5432 -> "PostgreSQL database port. Restrict access in pg_hba.conf to trusted IP ranges and require SSL/TLS connections."
        6379 -> "Redis in-memory store. By default lacks authentication or encryption. Bind to 127.0.0.1, set strong requirepass, and disable dangerous commands."
        5900 -> "VNC Remote Display server exposes desktop frames and inputs. Enforce strong authentication and tunnel through SSH or VPN."
        8080, 8443 -> "Alternate HTTP/HTTPS proxy or admin service. Inspect exposed debug endpoints (e.g. Spring Actuator) and default passwords."
        27017 -> "MongoDB database port exposed. Verify authorization is enabled (auth = true) and TLS encryption is active."
        else -> "Verify if this service requires public accessibility. Apply principle of least privilege and strict firewall ingress filtering."
    }
}

@Composable
fun PortDetailDialog(
    port: DisplayPort,
    target: String,
    rawBanner: String?,
    rawService: String?,
    accent: Color,
    lang: String,
    isMobile: Boolean = false,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val (riskLabel, riskColor, riskIcon) = getPortRiskLevel(port.number, lang)
    val advisory = getPortSecurityAdvisory(port.number, lang)
    val dossier = remember(port.number, rawBanner) { PortIntelligence.getDossier(port.number, rawBanner) }
    val dialogScrollState = rememberScrollState()
    var copiedAction by remember { mutableStateOf<String?>(null) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val isTouchEmulated = LocalTouchEmulation.current
    val shouldUseBottomSheet = isMobile || isTouchEmulated

    LaunchedEffect(copiedAction) {
        if (copiedAction != null) {
            kotlinx.coroutines.delay(2000)
            copiedAction = null
        }
    }

    val attackVectors = remember(dossier, lang) { dossier.getAttackVectors(lang) }
    val hardeningGuide = remember(dossier, lang) { dossier.getHardeningGuide(lang).ifBlank { advisory } }

    val copyDossier = {
        val fullDetails = buildString {
            appendLine("### [PortX Security Dossier] - Port ${port.number}/${port.title}")
            appendLine("- **${LocalizedStrings.get("port_detail_target", lang)}:** ${target}:${port.number}")
            appendLine("- **${LocalizedStrings.get("port_category", lang)}:** ${LocalizedStrings.get(dossier.categoryKey, lang)}")
            appendLine("- **${LocalizedStrings.get("port_tier", lang)}:** ${LocalizedStrings.get(dossier.tierKey, lang)}")
            appendLine("- **${LocalizedStrings.get("port_encryption", lang)}:** ${LocalizedStrings.get(dossier.encryptionKey, lang)}")
            if (dossier.rfcStandard.isNotBlank()) appendLine("- **${LocalizedStrings.get("port_rfc", lang)}:** ${dossier.rfcStandard}")
            appendLine("- **${LocalizedStrings.get("port_detail_risk", lang)}:** $riskLabel")
            appendLine("- **${LocalizedStrings.get("port_detail_banner", lang)}:** ${rawBanner ?: port.description}")
            if (attackVectors.isNotBlank()) {
                appendLine("\n#### ${LocalizedStrings.get("attack_vectors", lang)}:")
                appendLine(attackVectors)
            }
            if (dossier.cveReferences.isNotEmpty()) {
                appendLine("\n#### ${LocalizedStrings.get("common_cves", lang)}:")
                appendLine("  * " + dossier.cveReferences.joinToString(", "))
            }
            appendLine("\n#### ${LocalizedStrings.get("hardening_guide", lang)}:")
            appendLine(hardeningGuide)
        }
        clipboardManager.setText(AnnotatedString(fullDetails))
        isCopied = true
    }

    val headerContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.04f))
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = LocalizedStrings.get("close", lang),
                    tint = if (isDark) TextMuted else TextMutedLight,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            Brush.linearGradient(listOf(port.color.copy(alpha = 0.20f), port.color.copy(alpha = 0.06f))),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, port.color.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val portNumStr = if (lang == "fa") com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(port.number.toString()) else port.number.toString()
                    Text(
                        portNumStr,
                        color = port.color,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        fontSize = if (port.number > 9999) 10.sp else 11.5.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            port.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (dossier.rfcStandard.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = accent.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f)),
                                modifier = Modifier.widthIn(max = 135.dp)
                            ) {
                                Text(
                                    dossier.rfcStandard,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = accent,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    val targetText = if (lang == "fa") {
                        val faPort = com.mrcoder20.portx.domain.DateFormatter.toPersianDigits(port.number.toString())
                        "${LocalizedStrings.get("tcp_listening", lang)} \u2022 ${target}:${faPort}"
                    } else {
                        "\u2066${target}:${port.number}\u2069 \u2022 ${LocalizedStrings.get("tcp_listening", lang)}"
                    }
                    Text(
                        targetText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    val renderScrollableBody: @Composable ColumnScope.() -> Unit = {
        // Risk Level & State Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = riskColor.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.35f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(riskIcon, null, tint = riskColor, modifier = Modifier.size(14.dp))
                    Text(
                        text = riskLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = riskColor,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TertiaryNeon.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, TertiaryNeon.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(TertiaryNeon))
                    Text(
                        LocalizedStrings.get("port_open", lang),
                        color = TertiaryNeon,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    )
                }
            }
        }

        // Metadata Badges (Category, Tier, Encryption)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Category Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.25f) else BorderLight),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
                    Text(
                        LocalizedStrings.get("port_category", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1
                    )
                    Text(
                        LocalizedStrings.get(dossier.categoryKey, lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp),
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tier Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.25f) else BorderLight),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
                    Text(
                        LocalizedStrings.get("port_tier", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1
                    )
                    Text(
                        LocalizedStrings.get(dossier.tierKey, lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp),
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Encryption Chip
            val isCleartext = dossier.encryptionKey == "enc_cleartext"
            val encColor = if (isCleartext) DangerNeon else SecondaryNeon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = encColor.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, encColor.copy(alpha = 0.25f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
                    Text(
                        LocalizedStrings.get("port_encryption", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1
                    )
                    Text(
                        LocalizedStrings.get(dossier.encryptionKey, lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp),
                        color = encColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Quick Actions Row (URL, cURL, Nmap)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = {
                    val urlStr = if (port.number == 443 || port.number == 8443) "https://${target}:${port.number}" else "http://${target}:${port.number}"
                    clipboardManager.setText(AnnotatedString(urlStr))
                    copiedAction = "URL"
                },
                shape = RoundedCornerShape(8.dp),
                color = if (copiedAction == "URL") TertiaryNeon.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight),
                border = BorderStroke(1.dp, if (copiedAction == "URL") TertiaryNeon else (if (isDark) GlassBorder.copy(alpha = 0.35f) else BorderLight)),
                modifier = Modifier.weight(1f).height(30.dp).springPress(pressedScale = 0.94f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(if (copiedAction == "URL") Icons.Default.Check else Icons.Default.Link, null, tint = if (copiedAction == "URL") TertiaryNeon else accent, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (copiedAction == "URL") LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_url", lang),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1
                    )
                }
            }

            Surface(
                onClick = {
                    val curlCmd = "curl -v -m 5 http://${target}:${port.number}/"
                    clipboardManager.setText(AnnotatedString(curlCmd))
                    copiedAction = "CURL"
                },
                shape = RoundedCornerShape(8.dp),
                color = if (copiedAction == "CURL") TertiaryNeon.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight),
                border = BorderStroke(1.dp, if (copiedAction == "CURL") TertiaryNeon else (if (isDark) GlassBorder.copy(alpha = 0.35f) else BorderLight)),
                modifier = Modifier.weight(1.1f).height(30.dp).springPress(pressedScale = 0.94f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(if (copiedAction == "CURL") Icons.Default.Check else Icons.Default.Terminal, null, tint = if (copiedAction == "CURL") TertiaryNeon else SecondaryNeon, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (copiedAction == "CURL") LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_curl", lang),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1
                    )
                }
            }

            Surface(
                onClick = {
                    val nmapCmd = "nmap -sV -sC -p ${port.number} ${target}"
                    clipboardManager.setText(AnnotatedString(nmapCmd))
                    copiedAction = "NMAP"
                },
                shape = RoundedCornerShape(8.dp),
                color = if (copiedAction == "NMAP") TertiaryNeon.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight),
                border = BorderStroke(1.dp, if (copiedAction == "NMAP") TertiaryNeon else (if (isDark) GlassBorder.copy(alpha = 0.35f) else BorderLight)),
                modifier = Modifier.weight(0.9f).height(30.dp).springPress(pressedScale = 0.94f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(if (copiedAction == "NMAP") Icons.Default.Check else Icons.Default.Search, null, tint = if (copiedAction == "NMAP") TertiaryNeon else WarningNeon, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (copiedAction == "NMAP") LocalizedStrings.get("copied", lang) else "Nmap",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1
                    )
                }
            }
        }

        // Service & Banner Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color.Black.copy(alpha = 0.22f) else SurfaceInsetLight,
            border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.2f) else BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    LocalizedStrings.get("detected_banner", lang),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp, letterSpacing = 0.5.sp),
                    color = accent
                )
                val bannerText = if (!rawBanner.isNullOrBlank()) rawBanner.trim() else port.description
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, lineHeight = 15.sp),
                        color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Attack Surface & Threat Vectors Box
        if (attackVectors.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DangerNeon.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.BugReport, null, tint = DangerNeon, modifier = Modifier.size(13.dp))
                        Text(
                            LocalizedStrings.get("attack_vectors", lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                            color = DangerNeon
                        )
                    }
                    Text(
                        text = attackVectors,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
                        color = if (isDark) TextSecondary else TextSecondaryLight
                    )
                }
            }
        }

        // CVE Threat References
        if (dossier.cveReferences.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WarningNeon.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, WarningNeon.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Shield, null, tint = WarningNeon, modifier = Modifier.size(13.dp))
                        Text(
                            LocalizedStrings.get("common_cves", lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                            color = WarningNeon
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        dossier.cveReferences.forEach { cveId ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White,
                                border = BorderStroke(1.dp, WarningNeon.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(cveId))
                                    copiedAction = cveId
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = cveId,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = if (copiedAction == cveId) TertiaryNeon else (if (isDark) Color.White else Color.Black)
                                    )
                                    if (copiedAction == cveId) {
                                        Icon(Icons.Default.Check, null, tint = TertiaryNeon, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security & Hardening Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SecondaryNeon.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, SecondaryNeon.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Security, null, tint = SecondaryNeon, modifier = Modifier.size(13.dp))
                    Text(
                        LocalizedStrings.get("hardening_guide", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                        color = SecondaryNeon
                    )
                }
                Text(
                    text = hardeningGuide,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
                    color = if (isDark) TextSecondary else TextSecondaryLight
                )
            }
        }
    }

    if (shouldUseBottomSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                color = if (isDark) SurfaceDark else Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.2f), CircleShape)
                    )

                    Spacer(Modifier.height(8.dp))

                    headerContent()

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .touchDragScroll(dialogScrollState, isVertical = true)
                                .verticalScroll(dialogScrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            renderScrollableBody()
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.6f) else BorderLight)
                        ) {
                            Text(LocalizedStrings.get("close", lang), fontSize = 11.sp, color = if (isDark) Color.White else Color.Black)
                        }

                        Button(
                            onClick = copyDossier,
                            modifier = Modifier.weight(1.4f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            Spacer(Modifier.width(5.dp))
                            Text(if (isCopied) LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_details", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = if (isDark) SurfaceDark else Color.White,
            modifier = Modifier.then(
                if (isDark) Modifier else Modifier.shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0x280F172A),
                    spotColor = Color(0x380F172A)
                )
            ).padding(16.dp).widthIn(max = 540.dp),
            shape = RoundedCornerShape(20.dp),
            title = headerContent,
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .touchDragScroll(dialogScrollState, isVertical = true)
                            .verticalScroll(dialogScrollState)
                            .absolutePadding(
                                right = if (isRtl) 10.dp else 0.dp,
                                left = if (!isRtl) 10.dp else 0.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        renderScrollableBody()
                    }

                    PortXScrollStateVerticalScrollbar(
                        scrollState = dialogScrollState,
                        modifier = Modifier.align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft).fillMaxHeight()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = copyDossier,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(Modifier.width(5.dp))
                    Text(if (isCopied) LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_details", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.6f) else BorderLight),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(LocalizedStrings.get("close", lang), fontSize = 11.sp, color = if (isDark) Color.White else Color.Black)
                }
            }
        )
    }
}

