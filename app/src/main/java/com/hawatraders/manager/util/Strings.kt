package com.hawatraders.manager.util

/**
 * All user-visible Bengali text. Kept in code (not strings.xml) so ViewModels and
 * repositories can produce friendly messages without needing a Context.
 */
object Strings {
    const val appName = "হাওয়া ট্রেডার্স ম্যানেজার"

    // Navigation / generic
    const val tabSearch = "খুঁজুন"
    const val tabSettings = "সেটিং"
    const val cancel = "বাতিল"
    const val back = "পেছনে যান"
    const val wait = "অপেক্ষা করুন..."
    const val genericError = "কিছু একটা ভুল হয়েছে। আবার চেষ্টা করুন।"
    const val showPassword = "পাসওয়ার্ড দেখান"
    const val hidePassword = "পাসওয়ার্ড লুকান"
    const val productImage = "পণ্যের ছবি"

    // Search
    const val searchHint = "পণ্যের নাম লিখুন..."
    const val clearSearch = "লেখা মুছুন"
    const val filterAll = "সব"
    const val emptyNoProducts = "এখনও কোনো পণ্য নেই, সেটিং থেকে যোগ করুন"
    const val emptyNoResults = "কোনো পণ্য পাওয়া যায়নি"
    const val detailCompany = "কোম্পানি"
    const val detailCategory = "ধরন"
    const val detailUnit = "একক"
    const val closeSheet = "বন্ধ করুন"

    // Settings gate
    const val ownerArea = "মালিকের এলাকা"
    const val createPasswordTitle = "পাসওয়ার্ড তৈরি করুন"
    const val createPasswordHint =
        "শুধু মালিক পণ্য যোগ, সম্পাদনা বা মুছতে পারবেন। একটি পাসওয়ার্ড দিন (কমপক্ষে ৪ অক্ষর)।"
    const val password = "পাসওয়ার্ড"
    const val confirmPassword = "পাসওয়ার্ড আবার লিখুন"
    const val setPassword = "পাসওয়ার্ড সেট করুন"
    const val enterPasswordHint = "পণ্য যোগ বা পরিবর্তন করতে মালিকের পাসওয়ার্ড দিন।"
    const val unlock = "খুলুন"
    const val lock = "তালা দিন"
    const val passwordMismatch = "দুইটি পাসওয়ার্ড মেলেনি"
    fun passwordTooShort(min: Int) = "কমপক্ষে ${Bn.number(min)} অক্ষর দিন"
    fun wrongPassword(left: Int) = "ভুল পাসওয়ার্ড। আর ${Bn.number(left)} বার চেষ্টা করা যাবে।"
    fun lockedFor(seconds: Long) =
        "অনেকবার ভুল হয়েছে। ${Bn.number(seconds)} সেকেন্ড পরে আবার চেষ্টা করুন।"

    // Settings menu
    const val addProduct = "নতুন পণ্য যোগ করুন"
    const val addProductSub = "দাম ও ছবিসহ নতুন পণ্য"
    const val manageProducts = "পণ্য ব্যবস্থাপনা"
    const val manageProductsSub = "সম্পাদনা, দাম পরিবর্তন ও মুছে ফেলা"
    const val changePassword = "পাসওয়ার্ড পরিবর্তন"
    const val changePasswordSub = "সেটিং-এর পাসওয়ার্ড বদলান"
    const val backup = "ডেটা ব্যাকআপ"
    const val backupSub = "এনক্রিপ্টেড ব্যাকআপ ফাইল তৈরি করুন"
    const val importTitle = "ডেটা ইমপোর্ট"
    const val importSub = "ব্যাকআপ ফাইল (.htb) থেকে তথ্য ফিরিয়ে আনুন। মালিকের পাসওয়ার্ড লাগবে না।"
    const val pickFile = "ফাইল বাছাই করুন"

    // Product edit
    const val newProduct = "নতুন পণ্য"
    const val editProduct = "পণ্য সম্পাদনা"
    const val nameBn = "পণ্যের নাম (বাংলা) *"
    const val nameEn = "পণ্যের নাম (ইংরেজি)"
    const val company = "কোম্পানি"
    const val category = "ধরন"
    const val unit = "একক (যেমন: কেজি, লিটার) *"
    val unitSuggestions = listOf("কেজি", "গ্রাম", "লিটার", "মি.লি.", "প্যাকেট", "বস্তা", "বোতল", "পিস")
    const val price = "দাম (টাকা) *"
    const val errNameRequired = "বাংলা নাম লিখুন"
    const val errUnitRequired = "একক লিখুন"
    const val errPriceInvalid = "সঠিক দাম লিখুন (০ এর বেশি)"
    const val takePhoto = "ক্যামেরা"
    const val pickPhoto = "গ্যালারি"
    const val removePhoto = "ছবি মুছুন"
    const val noPhoto = "ছবি নেই"
    const val save = "সংরক্ষণ করুন"
    const val saveFailed = "সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।"
    const val productNotFound = "পণ্যটি খুঁজে পাওয়া যায়নি"
    const val imageFailed = "ছবিটি প্রক্রিয়া করা যায়নি। অন্য ছবি চেষ্টা করুন।"
    const val imageMissing = "ছবিটি পাওয়া যায়নি, আবার বাছাই করুন।"

    // Product management
    const val manageTitle = "পণ্য ব্যবস্থাপনা"
    const val searchProducts = "পণ্য খুঁজুন"
    const val edit = "সম্পাদনা"
    const val quickPrice = "দাম বদলান"
    const val newPrice = "নতুন দাম"
    const val delete = "মুছুন"
    const val confirmDeleteTitle = "পণ্য মুছবেন?"
    fun confirmDeleteBody(name: String) = "“$name” স্থায়ীভাবে মুছে যাবে। এটি আর ফেরত আনা যাবে না।"
    const val update = "হালনাগাদ"
    const val deleted = "পণ্য মুছে ফেলা হয়েছে"
    const val deleteFailed = "মুছে ফেলা যায়নি"
    const val priceUpdated = "দাম হালনাগাদ হয়েছে"
    const val newProductFab = "নতুন পণ্য"

    // Change password
    const val currentPassword = "বর্তমান পাসওয়ার্ড"
    const val newPassword = "নতুন পাসওয়ার্ড"
    const val confirmNewPassword = "নতুন পাসওয়ার্ড আবার লিখুন"
    const val changeAction = "পরিবর্তন করুন"
    const val passwordChanged = "পাসওয়ার্ড পরিবর্তন হয়েছে"

    // Backup
    const val backupTitle = "ডেটা ব্যাকআপ"
    const val backupWarning = "ব্যাকআপের পাসওয়ার্ড ভুলে গেলে ফাইল আর খোলা যাবে না।"
    const val backupPassword = "ব্যাকআপের পাসওয়ার্ড"
    const val confirmBackupPassword = "ব্যাকআপের পাসওয়ার্ড আবার লিখুন"
    const val createBackup = "ব্যাকআপ তৈরি করুন"
    const val creatingBackup = "ব্যাকআপ তৈরি হচ্ছে..."
    const val backupReady = "ব্যাকআপ ফাইল তৈরি হয়েছে"
    const val saveToFile = "ফাইলে সেভ করুন"
    const val share = "শেয়ার করুন"
    const val backupSaved = "ব্যাকআপ ফাইল সেভ হয়েছে"
    const val backupFailed = "ব্যাকআপ তৈরি করা যায়নি।"
    const val fileSaveFailed = "ফাইল সেভ করা যায়নি।"
    fun backupPasswordShort(min: Int) = "ব্যাকআপের পাসওয়ার্ড কমপক্ষে ${Bn.number(min)} অক্ষরের হতে হবে"

    // Import
    const val importPasswordTitle = "ব্যাকআপের পাসওয়ার্ড দিন"
    const val importVerify = "যাচাই করুন"
    const val importConfirmTitle = "ইমপোর্ট নিশ্চিত করুন"
    const val importOverwriteWarning = "পুরোনো সব তথ্য মুছে নতুন তথ্য বসবে।"
    fun importCount(n: Int) = "ব্যাকআপে ${Bn.number(n)}টি পণ্য আছে।"
    const val importDo = "ইমপোর্ট করুন"
    const val importVerifying = "ফাইল যাচাই হচ্ছে..."
    const val importRestoring = "তথ্য বসানো হচ্ছে..."
    fun importDone(n: Int) = "ইমপোর্ট সম্পন্ন: ${Bn.number(n)}টি পণ্য।"
    const val errWrongBackupPassword = "পাসওয়ার্ড ভুল অথবা ফাইলটি নষ্ট হয়ে গেছে।"
    const val errNotBackup = "এটি হাওয়া ট্রেডার্সের ব্যাকআপ ফাইল নয়।"
    const val errBackupCorrupt = "ব্যাকআপ ফাইলের তথ্য ঠিক নেই।"
    const val errFileTooLarge = "ফাইলটি অনেক বড়।"
    const val errReadFile = "ফাইলটি পড়া যায়নি।"
    const val errRestore = "তথ্য বসানো যায়নি। আগের তথ্য অপরিবর্তিত আছে।"
    const val errUnsupportedVersion = "এই ব্যাকআপ ফাইলের সংস্করণ সমর্থিত নয়।"
}
