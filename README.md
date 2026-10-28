# Compact Style (Forge 1.20.1)

ซอร์สของ mod Compact Style: ไอเท็มสไตล์การต่อสู้ทั้งหมดอยู่ในแท็บ Creative ของตัวเอง
ไอเท็มเป็น Item ธรรมดา วางบล็อกไม่ได้ และผูกกับท่าของ Epic Fight ผ่านไฟล์ใน data/compact_style/capabilities/weapons/

ยังไม่ได้ build หรือทดสอบ เป็นซอร์สที่เขียนโดยไม่ได้คอมไพล์

## วิธี build
1. ดาวน์โหลด Forge MDK สำหรับ Minecraft 1.20.1 (Forge 47.x) แล้วแตก zip
2. ใน gradle.properties ตั้งค่า
   - mod_id=compact_style
   - mod_name=Compact Style
   - mod_group_id=com.compactstyle
3. ลบโฟลเดอร์ src ของ MDK ทิ้ง แล้วคัดลอกโฟลเดอร์ src ของโปรเจกต์นี้ไปแทน
4. วาง jar ของ Epic Fight 20.14.17 ใน run/mods ตอนทดสอบ ตัวโค้ดไม่ได้ import คลาสของ Epic Fight
5. รัน `./gradlew build` ไฟล์ jar จะอยู่ที่ build/libs/

## ไอเท็มในแท็บ Compact Style
- compact_style:classic หมัดโกง ไอคอนวงกลมมี X ตอนถือมองไม่เห็นในมือ
- compact_style:muaythai_gloves ถุงมือมวยไทย
- compact_style:chinese_style สไตล์จีน
- compact_style:taekwondo_style สไตล์เทควันโด

## สิ่งที่ยังเป็นของชั่วคราว
chinese_style และ taekwondo_style ตอนนี้ใช้ท่าหมัดมาตรฐาน (`epicfight:fist`) เป็นตัวแทน
เมื่อรู้ ID ของ weapon type จากมอด Fighting Styles ให้แก้ค่า `"type"` ในไฟล์
data/compact_style/capabilities/weapons/chinese_style.json และ taekwondo_style.json
