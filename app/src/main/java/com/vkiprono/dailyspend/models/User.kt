package com.vkiprono.dailyspend.models

class User(val uername:String, val password:String, val imgUri:String, val email:String) {
    constructor():this("","","","")
}