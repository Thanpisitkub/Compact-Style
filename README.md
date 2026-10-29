# Compact Style

Mod สไตล์การต่อสู้สำหรับ Epic Fight (Minecraft 1.20.1, Forge) เวอร์ชัน 2.1.1
อยู่ในแท็บ Creative ชื่อ **Compact Style**

ต้องติดตั้งคู่กัน: Epic Fight 20.14.17 และ Weapons of Miracles 2.0.171

## ไอเท็ม
| ไอเท็ม | ท่า |
|---|---|
| คลาสสิค (`compact_style:classic`) | Agony เต็มชุด ค่าสถานะแรงมาก ตอนถือมองไม่เห็นในมือ |
| ถุงมือมวยไทย (`compact_style:muaythai_gloves`) | Ender Blaster มือเดียว (หมัด เข่า เตะ) |
| สไตล์จีน (`compact_style:chinese_style`) | ผสม Satsujin, Orbit, Moonless, Nova |
| สไตล์เทควันโด (`compact_style:taekwondo_style`) | Ender Blaster สองมือ ผสม Nova |

ทุกสไตล์ยืน/เดิน/วิ่ง/ป้องกันแบบ Agony ส่วนคอมโบโจมตีต่างกัน
แก้ท่าได้ใน `src/main/java/com/compactstyle/CompactPresets.java`

## ดาวน์โหลด jar
ดาวน์โหลดจากหน้า Releases (รุ่น latest-jar) หรือแท็บ Actions > Artifacts
เมื่อ push ขึ้น main workflow จะ build ให้อัตโนมัติ หรือกดรันเองด้วย "Run workflow"

## หมายเหตุ
ซอร์สนี้ยังไม่เคย build หรือทดสอบในเกม ถ้า build ไม่ผ่านให้ดู log ในแท็บ Actions
workflow ดาวน์โหลดไฟล์ jar ของ Epic Fight และ Weapons of Miracles จาก Modrinth
ตอน build (ไม่เก็บไฟล์เหล่านั้นไว้ใน repo)
