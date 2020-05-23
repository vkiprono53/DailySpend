package com.vkiprono.dailyspend.models

class ItemSpend(val item:String, val description:String, val date:String,val amount:Int) {
    constructor():this("","","",0)
}