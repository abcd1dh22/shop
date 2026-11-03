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
    const val heroSubtitle = "পণ্যের দাম এক নজরে"
    fun resultCount(n: Int) = "${Bn.number(n)}টি পণ্য"
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
    const val sectionOwner = "মালিকের টুলস"
    const val sectionEveryone = "সবার জন্য"
    const val ownerOpen = "মালিকের এলাকা খোলা আছে"
    const val ownerLocked = "মালিকের এলাকা তালাবদ্ধ"
    const val sectionPhoto = "ছবি"
    const val sectionInfo = "পণ্যের তথ্য"
    const val sectionPrice = "দাম ও একক"
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

    // ---- v2 features ----
    const val filterFavorites = "পছন্দের"
    const val favoriteAdd = "পছন্দের তালিকায় যোগ করুন"
    const val favoriteRemove = "পছন্দের তালিকা থেকে সরান"
    const val favoriteSwitch = "পছন্দের পণ্য (তালিকার সবার আগে দেখাবে)"
    fun priceDate(text: String) = "দাম হালনাগাদ: $text"
    const val sectionNote = "ব্যবহারের নির্দেশনা"
    const val noteHint = "মাত্রা, প্রয়োগের নিয়ম, সতর্কতা"
    const val largeText = "বড় লেখার মোড"
    const val largeTextSub = "দোকানে সহজে পড়ার জন্য সব লেখা বড় হবে"

    // Bulk price change
    const val bulkTitle = "একসাথে দাম বদল"
    const val bulkSub = "সব বা বাছাই করা পণ্যের দাম শতাংশ বা টাকায় বাড়ান/কমান"
    const val bulkWhich = "কোন পণ্য"
    const val bulkCompanyHint = "কোম্পানির নাম (ফাঁকা থাকলে সব)"
    const val bulkHow = "কীভাবে বদলাবেন"
    const val bulkIncrease = "বাড়ান"
    const val bulkDecrease = "কমান"
    const val bulkPercent = "শতাংশ (%)"
    const val bulkTaka = "টাকা (৳)"
    const val bulkAmount = "পরিমাণ"
    const val bulkAmountInvalid = "সঠিক পরিমাণ লিখুন (০ এর বেশি)"
    const val bulkRoundNote = "নতুন দাম পূর্ণ টাকায় গোল করা হবে।"
    const val bulkPreview = "প্রিভিউ"
    const val bulkNone = "কোনো পণ্য মেলেনি"
    fun bulkAffected(n: Int) = "${Bn.number(n)}টি পণ্যের দাম বদলাবে"
    fun bulkMore(n: Int) = "…আরও ${Bn.number(n)}টি"
    const val bulkApply = "প্রয়োগ করুন"
    const val bulkConfirmTitle = "দাম বদল নিশ্চিত করুন"
    fun bulkConfirmBody(n: Int) =
        "${Bn.number(n)}টি পণ্যের দাম বদলে যাবে। আগের দামে ফেরানো যাবে না, তাই আগে ব্যাকআপ নিয়ে রাখতে পারেন।"
    fun bulkDone(n: Int) = "${Bn.number(n)}টি পণ্যের দাম হালনাগাদ হয়েছে"

    // CSV / Excel import
    const val importProductsTitle = "CSV/Excel থেকে ইমপোর্ট"
    const val importProductsSub = "ফাইল থেকে অনেক পণ্য একবারে যোগ বা দাম হালনাগাদ করুন"
    const val importHelp =
        "ফাইলের প্রথম সারিতে কলামের নাম থাকতে হবে।\n" +
            "আবশ্যক: নাম, দাম\n" +
            "ঐচ্ছিক: ইংরেজি নাম, কোম্পানি, ধরন, একক, নির্দেশনা\n\n" +
            "একই নাম ও কোম্পানির পণ্য আগে থেকে থাকলে তার তথ্য হালনাগাদ হবে, না থাকলে নতুন যোগ হবে। " +
            "কিছু মোছা হবে না। ছবি ইমপোর্ট হয় না।"
    const val importTemplate = "নমুনা ফাইল (CSV)"
    const val importExportCurrent = "বর্তমান পণ্য (CSV)"
    const val importPickSheet = "CSV/Excel ফাইল বাছাই করুন"
    const val importReading = "ফাইল পড়া হচ্ছে..."
    const val importApplying = "পণ্য সংরক্ষণ হচ্ছে..."
    const val importPreviewTitle = "ইমপোর্টের সারসংক্ষেপ"
    fun importPlan(newCount: Int, updateCount: Int) =
        "নতুন যোগ হবে: ${Bn.number(newCount)}টি\nহালনাগাদ হবে: ${Bn.number(updateCount)}টি"
    fun importSkipped(n: Int) = "বাদ পড়েছে (ত্রুটি): ${Bn.number(n)}টি সারি"
    fun importLine(line: Int, msg: String) = "সারি ${Bn.number(line)}: $msg"
    fun importApplied(n: Int) = "ইমপোর্ট সম্পন্ন: ${Bn.number(n)}টি পণ্য"
    const val importNothing = "ইমপোর্ট করার মতো সঠিক সারি নেই।"
    const val errMissingColumns = "প্রথম সারিতে “নাম” ও “দাম” কলাম থাকতে হবে।"
    const val errEmptyFile = "ফাইলে কোনো পণ্য পাওয়া যায়নি।"
    const val errXls = "পুরোনো .xls ফাইল সমর্থিত নয়। Excel-এ Save As করে .xlsx অথবা CSV বানান।"
    const val errTooManyRows = "ফাইলে অনেক বেশি সারি (সর্বোচ্চ ২০,০০০)।"
    const val rowNoName = "নাম নেই"
    const val rowBadPrice = "দাম ঠিক নেই"
    const val rowDuplicate = "ফাইলে একই পণ্য আগেই আছে"

    // Fingerprint unlock
    const val bioTitle = "মালিকের এলাকা খুলুন"
    const val bioSubtitle = "আঙুলের ছাপ দিয়ে নিশ্চিত করুন"
    const val bioUsePassword = "পাসওয়ার্ড দিন"
    const val bioButton = "ফিঙ্গারপ্রিন্ট দিয়ে খুলুন"
    const val bioSwitchTitle = "ফিঙ্গারপ্রিন্ট দিয়ে খুলুন"
    const val bioSwitchSub = "ফোনে যাদের আঙুলের ছাপ সেট করা আছে, তারা সবাই মালিকের এলাকা খুলতে পারবে।"
    const val bioNotAvailable = "এই ফোনে আঙুলের ছাপ সেট করা নেই। আগে ফোনের সেটিংসে সেট করুন।"
    const val bioLockout = "অনেকবার ভুল হয়েছে। পাসওয়ার্ড দিয়ে খুলুন।"
    const val bioEnabledMsg = "ফিঙ্গারপ্রিন্ট আনলক চালু হয়েছে"
}
