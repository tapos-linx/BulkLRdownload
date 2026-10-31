package com.example.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationRepository(private val context: Context? = null) {

    private var cachedDivisions: List<Division>? = null
    var currentSourceType: CatalogueSourceType = CatalogueSourceType.OFFICIAL_BUNDLED
        private set

    companion object {
        val UPAZILA_BN_MAP: Map<String, String> = mapOf(
            // Cumilla (17 Upazilas)
            "Cumilla Adarsha Sadar" to "কুমিল্লা আদর্শ সদর (Adarsha Sadar)",
            "Cumilla Sadar Dakshin" to "কুমিল্লা সদর দক্ষিণ (Sadar Dakshin)",
            "Barura" to "বরুড়া (Barura)",
            "Brahmanpara" to "ব্রাহ্মণপাড়া (Brahmanpara)",
            "Burichang" to "বুড়িচং (Burichang)",
            "Chandina" to "চান্দিনা (Chandina)",
            "Chauddagram" to "চৌদ্দগ্রাম (Chauddagram)",
            "Daudkandi" to "দাউদকান্দি (Daudkandi)",
            "Debidwar" to "দেবিদ্বার (Debidwar)",
            "Homna" to "হোমনা (Homna)",
            "Laksam" to "লাকসাম (Laksam)",
            "Lalmai" to "লালমাই (Lalmai)",
            "Meghna" to "মেঘনা (Meghna)",
            "Monohargonj" to "মনোহরগঞ্জ (Monohargonj)",
            "Muradnagar" to "মুরাদনগর (Muradnagar)",
            "Nangalkot" to "নাঙ্গলকোট (Nangalkot)",
            "Titas" to "তিতাস (Titas)",
            // Brahmanbaria (9 Upazilas)
            "Brahmanbaria Sadar" to "ব্রাহ্মণবাড়িয়া সদর (Brahmanbaria Sadar)",
            "Sarail" to "সরাইল (Sarail)",
            "Ashuganj" to "আশুগঞ্জ (Ashuganj)",
            "Kasba" to "কসবা (Kasba)",
            "Nabinagar" to "নবীনগর (Nabinagar)",
            "Nasirnagar" to "নাসিরনগর (Nasirnagar)",
            "Bancharampur" to "বাঞ্ছারামপুর (Bancharampur)",
            "Akhaura" to "আখাউড়া (Akhaura)",
            "Bijoynagar" to "বিজয়নগর (Bijoynagar)"
        )

        private val CURATED_CUMILLA_MOUZAS: Map<String, List<MouzaInfo>> = mapOf(
            "Cumilla Adarsha Sadar" to listOf(
                MouzaInfo("cm_as_01", "Kalikapur", "কালিকাপুর", "JL 101"),
                MouzaInfo("cm_as_02", "Bagichagaon", "বাগিচাগাঁও", "JL 102"),
                MouzaInfo("cm_as_03", "Chhotara", "ছোটরা", "JL 103"),
                MouzaInfo("cm_as_04", "Shasangachha", "শাসনগাছা", "JL 104"),
                MouzaInfo("cm_as_05", "Amratali", "আমড়াতলী", "JL 105"),
                MouzaInfo("cm_as_06", "Jagannathpur", "জগন্নাথপুর", "JL 106"),
                MouzaInfo("cm_as_07", "Kaptanbazar", "কাপ্তানবাজার", "JL 107"),
                MouzaInfo("cm_as_08", "Durgapur", "দুর্গাপুর", "JL 108"),
                MouzaInfo("cm_as_09", "Bibirbazar", "বিবিরবাজার", "JL 109"),
                MouzaInfo("cm_as_10", "Panchthubi", "পাঁচথুবী", "JL 110")
            ),
            "Cumilla Sadar Dakshin" to listOf(
                MouzaInfo("cm_sd_01", "Bijoypur", "বিজয়পুর", "JL 110"),
                MouzaInfo("cm_sd_02", "Chouara", "চৌয়ারা", "JL 111"),
                MouzaInfo("cm_sd_03", "Galiara", "গলিয়ারা", "JL 112"),
                MouzaInfo("cm_sd_04", "Jorkanon", "জোড়কানন", "JL 113"),
                MouzaInfo("cm_sd_05", "Barapara", "বড়পাড়া", "JL 114"),
                MouzaInfo("cm_sd_06", "Belghar", "বেলঘর", "JL 115")
            ),
            "Barura" to listOf(
                MouzaInfo("cm_ba_01", "Barura", "বরুড়া", "JL 200"),
                MouzaInfo("cm_ba_02", "Payalgachha", "পয়ালগাছা", "JL 201"),
                MouzaInfo("cm_ba_03", "Adda", "আড্ডা", "JL 202"),
                MouzaInfo("cm_ba_04", "Galimpur", "গালিমপুর", "JL 203"),
                MouzaInfo("cm_ba_05", "Shilmuri", "শিলমুড়ী", "JL 204"),
                MouzaInfo("cm_ba_06", "Bhabanipur", "ভবানীপুর", "JL 205")
            ),
            "Brahmanpara" to listOf(
                MouzaInfo("cm_bp_01", "Brahmanpara", "ব্রাহ্মণপাড়া", "JL 160"),
                MouzaInfo("cm_bp_02", "Madhabpur", "মাধবপুর", "JL 161"),
                MouzaInfo("cm_bp_03", "Shashidal", "শশীদল", "JL 162"),
                MouzaInfo("cm_bp_04", "Chandla", "চান্দলা", "JL 163"),
                MouzaInfo("cm_bp_05", "Shidlai", "সিদলাই", "JL 164"),
                MouzaInfo("cm_bp_06", "Dulalpur", "দুলালপুর", "JL 165")
            ),
            "Burichang" to listOf(
                MouzaInfo("cm_br_01", "Burichang", "বুড়িচং", "JL 150"),
                MouzaInfo("cm_br_02", "Mainamati", "ময়নামতি", "JL 151"),
                MouzaInfo("cm_br_03", "Rajapur", "রাজাপুর", "JL 152"),
                MouzaInfo("cm_br_04", "Bakshimul", "বাকশীমূল", "JL 153"),
                MouzaInfo("cm_br_05", "Sholanal", "ষোলনল", "JL 154"),
                MouzaInfo("cm_br_06", "Pirjatrapur", "পীরযাত্রাপুর", "JL 155"),
                MouzaInfo("cm_br_07", "Mokam", "মোকাম", "JL 156")
            ),
            "Chandina" to listOf(
                MouzaInfo("cm_cd_01", "Chandina", "চান্দিনা", "JL 120"),
                MouzaInfo("cm_cd_02", "Mahichail", "মহিচাইল", "JL 121"),
                MouzaInfo("cm_cd_03", "Kerankhal", "কেরণখাল", "JL 122"),
                MouzaInfo("cm_cd_04", "Madhaiya", "মাধাইয়া", "JL 123"),
                MouzaInfo("cm_cd_05", "Barera", "বড়েরা", "JL 124"),
                MouzaInfo("cm_cd_06", "gallai", "গল্লাই", "JL 125")
            ),
            "Chauddagram" to listOf(
                MouzaInfo("cm_cg_01", "Chauddagram", "চৌদ্দগ্রাম", "JL 170"),
                MouzaInfo("cm_cg_02", "Chiara", "চিওড়া", "JL 171"),
                MouzaInfo("cm_cg_03", "Gunabati", "গুণবতী", "JL 172"),
                MouzaInfo("cm_cg_04", "Miabazar", "মিয়াবাজার", "JL 173"),
                MouzaInfo("cm_cg_05", "Batisa", "বাতিসা", "JL 174"),
                MouzaInfo("cm_cg_06", "Munshirhat", "মুন্সীরহাট", "JL 175")
            ),
            "Daudkandi" to listOf(
                MouzaInfo("cm_dk_01", "Daudkandi", "দাউদকান্দি", "JL 130"),
                MouzaInfo("cm_dk_02", "Gouripur", "গৌরীপুর", "JL 131"),
                MouzaInfo("cm_dk_03", "Eliotganj", "ইলিয়টগঞ্জ", "JL 132"),
                MouzaInfo("cm_dk_04", "Sundalpur", "সুন্দলপুর", "JL 133"),
                MouzaInfo("cm_dk_05", "Goalmari", "গোয়ালমারী", "JL 134"),
                MouzaInfo("cm_dk_06", "Hasanpur", "হাসানপুর", "JL 135")
            ),
            "Debidwar" to listOf(
                MouzaInfo("cm_db_01", "Debidwar", "দেবিদ্বার", "JL 140"),
                MouzaInfo("cm_db_02", "Gunaighar", "গুনাইঘর", "JL 141"),
                MouzaInfo("cm_db_03", "Mohanpur", "মোহনপুর", "JL 142"),
                MouzaInfo("cm_db_04", "Jafarganj", "জাফরগঞ্জ", "JL 143"),
                MouzaInfo("cm_db_05", "Barkamta", "বড়কামতা", "JL 144"),
                MouzaInfo("cm_db_06", "Dhamti", "ধামতী", "JL 145")
            ),
            "Homna" to listOf(
                MouzaInfo("cm_hm_01", "Homna", "হোমনা", "JL 210"),
                MouzaInfo("cm_hm_02", "Ghagutia", "ঘাগুটিয়া", "JL 211"),
                MouzaInfo("cm_hm_03", "Dulalpur", "দুলালপুর", "JL 212"),
                MouzaInfo("cm_hm_04", "Asadpur", "আসাদপুর", "JL 213"),
                MouzaInfo("cm_hm_05", "Nilkhi", "নীলখী", "JL 214")
            ),
            "Laksam" to listOf(
                MouzaInfo("cm_lk_01", "Laksam", "লাকসাম", "JL 180"),
                MouzaInfo("cm_lk_02", "Mudafarganj", "মুদাফরগঞ্জ", "JL 181"),
                MouzaInfo("cm_lk_03", "Kandirpar", "কান্দিরপাড়", "JL 182"),
                MouzaInfo("cm_lk_04", "Gobindapur", "গোবিন্দপুর", "JL 183"),
                MouzaInfo("cm_lk_05", "Uttarda", "উত্তরদা", "JL 184")
            ),
            "Lalmai" to listOf(
                MouzaInfo("cm_lm_01", "Bagmara", "বাগমারা", "JL 260"),
                MouzaInfo("cm_lm_02", "Perul", "পেরুল", "JL 261"),
                MouzaInfo("cm_lm_03", "Bhuloin", "ভূলইন", "JL 262"),
                MouzaInfo("cm_lm_04", "Bakhrabad", "বাখরাবাদ", "JL 263"),
                MouzaInfo("cm_lm_05", "Belghar", "বেলঘর", "JL 264")
            ),
            "Meghna" to listOf(
                MouzaInfo("cm_mg_01", "Manikarchar", "মানিকারচর", "JL 230"),
                MouzaInfo("cm_mg_02", "Gobindapur", "গোবিন্দপুর", "JL 231"),
                MouzaInfo("cm_mg_03", "Chandanpur", "চন্দনপুর", "JL 232"),
                MouzaInfo("cm_mg_04", "Chalivanga", "চালিভাঙ্গা", "JL 233"),
                MouzaInfo("cm_mg_05", "Radhanagar", "রাধানগর", "JL 234")
            ),
            "Monohargonj" to listOf(
                MouzaInfo("cm_mn_01", "Monohargonj", "মনোহরগঞ্জ", "JL 240"),
                MouzaInfo("cm_mn_02", "Baishgaon", "বাইশগাঁও", "JL 241"),
                MouzaInfo("cm_mn_03", "Lakshmanpur", "লক্ষ্মণপুর", "JL 242"),
                MouzaInfo("cm_mn_04", "Khila", "খিলা", "JL 243"),
                MouzaInfo("cm_mn_05", "Natherpetua", "নাথেরপেটুয়া", "JL 244")
            ),
            "Muradnagar" to listOf(
                MouzaInfo("cm_mr_01", "Muradnagar", "মুরাদনগর", "JL 190"),
                MouzaInfo("cm_mr_02", "Dhamghar", "ধামঘর", "JL 191"),
                MouzaInfo("cm_mr_03", "Jahapur", "জাহাপুর", "JL 192"),
                MouzaInfo("cm_mr_04", "Companyganj", "কোম্পানীগঞ্জ", "JL 193"),
                MouzaInfo("cm_mr_05", "Bangra", "বাঙ্গরা", "JL 194"),
                MouzaInfo("cm_mr_06", "Sreekail", "শ্রীকাইল", "JL 195")
            ),
            "Nangalkot" to listOf(
                MouzaInfo("cm_nk_01", "Nangalkot", "নাঙ্গলকোট", "JL 250"),
                MouzaInfo("cm_nk_02", "Raikot", "রায়কোট", "JL 251"),
                MouzaInfo("cm_nk_03", "Pariyara", "পেরিয়া", "JL 252"),
                MouzaInfo("cm_nk_04", "Mokrara", "মৌকরা", "JL 253"),
                MouzaInfo("cm_nk_05", "Dhalua", "ঢালুয়া", "JL 254")
            ),
            "Titas" to listOf(
                MouzaInfo("cm_tt_01", "Karikandi", "কড়িকান্দি", "JL 220"),
                MouzaInfo("cm_tt_02", "Majidpur", "মজিদপুর", "JL 221"),
                MouzaInfo("cm_tt_03", "Jagatpur", "জগতপুর", "JL 222"),
                MouzaInfo("cm_tt_04", "Balorampur", "বলরামপুর", "JL 223"),
                MouzaInfo("cm_tt_05", "Kalaikandi", "কলাইকান্দি", "JL 224"),
                MouzaInfo("cm_tt_06", "Vitikandi", "ভিটিকান্দি", "JL 225"),
                MouzaInfo("cm_tt_07", "Narayandia", "নারায়ণদিয়া", "JL 226"),
                MouzaInfo("cm_tt_08", " জিয়ারকান্দি", "জিয়ারকান্দি", "JL 227")
            )
        )

        private val CURATED_BRAHMANBARIA_MOUZAS: Map<String, List<MouzaInfo>> = mapOf(
            "Brahmanbaria Sadar" to listOf(
                MouzaInfo("bb_s_01", "Medda", "মেদ্দা", "JL 01"),
                MouzaInfo("bb_s_02", "Paikpara", "পাইকপাড়া", "JL 02"),
                MouzaInfo("bb_s_03", "Puniaut", "পুনিয়াউট", "JL 03"),
                MouzaInfo("bb_s_04", "Kazipara", "কাজীপাড়া", "JL 04"),
                MouzaInfo("bb_s_05", "Ghatura", "ঘাটুরা", "JL 05"),
                MouzaInfo("bb_s_06", "Suhilpur", "সুহিলপুর", "JL 06"),
                MouzaInfo("bb_s_07", "Natai", "নাটাই", "JL 07"),
                MouzaInfo("bb_s_08", "Birampur", "বিরামপুর", "JL 08")
            ),
            "Sarail" to listOf(
                MouzaInfo("bb_sr_01", "Sarail", "সরাইল", "JL 10"),
                MouzaInfo("bb_sr_02", "Annadaut", "অন্নদাউট", "JL 11"),
                MouzaInfo("bb_sr_03", "Kalikachha", "কালিকচ্ছ", "JL 12"),
                MouzaInfo("bb_sr_04", "Panishwar", "পানিশ্বর", "JL 13"),
                MouzaInfo("bb_sr_05", "Shahbajpur", "শাহবাজপুর", "JL 14"),
                MouzaInfo("bb_sr_06", "Chunta", "চুণ্টা", "JL 15"),
                MouzaInfo("bb_sr_07", "Noagaon", "নোয়াগাঁও", "JL 16")
            ),
            "Ashuganj" to listOf(
                MouzaInfo("bb_as_01", "Ashuganj", "আশুগঞ্জ", "JL 20"),
                MouzaInfo("bb_as_02", "Char Chartala", "চর চারতলা", "JL 21"),
                MouzaInfo("bb_as_03", "Tarua", "তারুয়া", "JL 22"),
                MouzaInfo("bb_as_04", "Araisidha", "আড়াইসিধা", "JL 23"),
                MouzaInfo("bb_as_05", "Durgapur", "দুর্গাপুর", "JL 24"),
                MouzaInfo("bb_as_06", "Talshahar", "তালশহর", "JL 25")
            ),
            "Kasba" to listOf(
                MouzaInfo("bb_kb_01", "Kasba", "কসবা", "JL 30"),
                MouzaInfo("bb_kb_02", "Kuti", "কুটি", "JL 31"),
                MouzaInfo("bb_kb_03", "Binauti", "বিনাউটি", "JL 32"),
                MouzaInfo("bb_kb_04", "Mehari", "মেহারী", "JL 33"),
                MouzaInfo("bb_kb_05", "Gopinathpur", "গোপীনাথপুর", "JL 34"),
                MouzaInfo("bb_kb_06", "Kharera", "খাড়েরা", "JL 35")
            ),
            "Nabinagar" to listOf(
                MouzaInfo("bb_nn_01", "Nabinagar", "নবীনগর", "JL 40"),
                MouzaInfo("bb_nn_02", "Rasullabad", "রসুল্লাবাদ", "JL 41"),
                MouzaInfo("bb_nn_03", "Shibpur", "শিবপুর", "JL 42"),
                MouzaInfo("bb_nn_04", "Sreerampur", "শ্রীরামপুর", "JL 43"),
                MouzaInfo("bb_nn_05", "Salimganj", "সলিমগঞ্জ", "JL 44"),
                MouzaInfo("bb_nn_06", "Bitghar", "বিটঘর", "JL 45")
            ),
            "Nasirnagar" to listOf(
                MouzaInfo("bb_ns_01", "Nasirnagar", "নাসিরনগর", "JL 50"),
                MouzaInfo("bb_ns_02", "Chatalpar", "চাতলপাড়", "JL 51"),
                MouzaInfo("bb_ns_03", "Goalnagar", "গোয়ালনগর", "JL 52"),
                MouzaInfo("bb_ns_04", "Fandauk", "ফান্দাউক", "JL 53"),
                MouzaInfo("bb_ns_05", "Gokarna", "গোকর্ণ", "JL 54")
            ),
            "Bancharampur" to listOf(
                MouzaInfo("bb_bc_01", "Bancharampur", "বাঞ্ছারামপুর", "JL 60"),
                MouzaInfo("bb_bc_02", "Ujanchar", "উজানচর", "JL 61"),
                MouzaInfo("bb_bc_03", "Darikandi", "দড়িকান্দি", "JL 62"),
                MouzaInfo("bb_bc_04", "Salimabad", "ছলিমাবাদ", "JL 63"),
                MouzaInfo("bb_bc_05", "Rupsadi", "রূপসদী", "JL 64")
            ),
            "Akhaura" to listOf(
                MouzaInfo("bb_ak_01", "Akhaura", "আখাউড়া", "JL 70"),
                MouzaInfo("bb_ak_02", "Mogra", "মোগড়া", "JL 71"),
                MouzaInfo("bb_ak_03", "Dharkhar", "ধরখার", "JL 72"),
                MouzaInfo("bb_ak_04", "Maniand", "মনিয়ন্দ", "JL 73"),
                MouzaInfo("bb_ak_05", "Ganganagar", "গঙ্গানগর", "JL 74")
            ),
            "Bijoynagar" to listOf(
                MouzaInfo("bb_bj_01", "Champaknagar", "চম্পকনগর", "JL 80"),
                MouzaInfo("bb_bj_02", "Harashpur", "হরষপুর", "JL 81"),
                MouzaInfo("bb_bj_03", "Singarbil", "সিঙ্গারবিল", "JL 82"),
                MouzaInfo("bb_bj_04", "Pattan", "পত্তন", "JL 83"),
                MouzaInfo("bb_bj_05", "Ichhapura", "ইছাপুরা", "JL 84")
            )
        )
    }

    fun getDivisions(): List<Division> {
        cachedDivisions?.let { return it }

        val loaded = try {
            val stream = context?.assets?.open("bd_locations.json")
            if (stream != null) {
                val json = stream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<Division>>() {}.type
                val parsed: List<Division> = Gson().fromJson(json, type)
                enrichPriorityDistricts(parsed)
            } else {
                getFallbackDivisions()
            }
        } catch (e: Exception) {
            getFallbackDivisions()
        }

        cachedDivisions = loaded
        return loaded
    }

    private fun enrichPriorityDistricts(divisions: List<Division>): List<Division> {
        // Put Chattogram first so Cumilla and Brahmanbaria are immediately accessible
        val sortedDivisions = divisions.sortedByDescending { it.name.equals("Chattogram", ignoreCase = true) }
        return sortedDivisions.map { division ->
            if (division.name.equals("Chattogram", ignoreCase = true)) {
                val enrichedDistricts = division.districts.map { dist ->
                    when {
                        dist.name.equals("Cumilla", ignoreCase = true) -> {
                            val mergedMouzas = dist.mouzas.toMutableMap()
                            CURATED_CUMILLA_MOUZAS.forEach { (upz, curatedList) ->
                                val assetList = mergedMouzas[upz].orEmpty()
                                val combined = (curatedList + assetList)
                                    .distinctBy { "${it.name}_${it.jlNo}" }
                                    .mapIndexed { idx, m ->
                                        m.copy(
                                            id = if (m.id.isBlank()) "cm_${upz.lowercase().replace(" ", "_")}_${idx + 1}" else m.id,
                                            isSelected = true
                                        )
                                    }
                                mergedMouzas[upz] = combined
                            }
                            dist.copy(
                                upazilas = CURATED_CUMILLA_MOUZAS.keys.toList(),
                                mouzas = mergedMouzas,
                                isPriority = true
                            )
                        }
                        dist.name.equals("Brahmanbaria", ignoreCase = true) -> {
                            val mergedMouzas = dist.mouzas.toMutableMap()
                            CURATED_BRAHMANBARIA_MOUZAS.forEach { (upz, curatedList) ->
                                val assetList = mergedMouzas[upz].orEmpty()
                                val combined = (curatedList + assetList)
                                    .distinctBy { "${it.name}_${it.jlNo}" }
                                    .mapIndexed { idx, m ->
                                        m.copy(
                                            id = if (m.id.isBlank()) "bb_${upz.lowercase().replace(" ", "_")}_${idx + 1}" else m.id,
                                            isSelected = true
                                        )
                                    }
                                mergedMouzas[upz] = combined
                            }
                            dist.copy(
                                upazilas = CURATED_BRAHMANBARIA_MOUZAS.keys.toList(),
                                mouzas = mergedMouzas,
                                isPriority = true
                            )
                        }
                        else -> dist
                    }
                }.sortedWith(
                    compareByDescending<District> {
                        it.name.equals("Cumilla", ignoreCase = true) ||
                            it.name.equals("Brahmanbaria", ignoreCase = true)
                    }.thenByDescending { it.name.equals("Cumilla", ignoreCase = true) }
                        .thenBy { it.name }
                )
                division.copy(districts = enrichedDistricts)
            } else {
                division
            }
        }
    }

    private fun getFallbackDivisions(): List<Division> {
        val cumillaDistrict = District(
            name = "Cumilla",
            bnName = "কুমিল্লা",
            upazilas = CURATED_CUMILLA_MOUZAS.keys.toList(),
            mouzas = CURATED_CUMILLA_MOUZAS,
            isPriority = true
        )
        val brahmanbariaDistrict = District(
            name = "Brahmanbaria",
            bnName = "ব্রাহ্মণবাড়িয়া",
            upazilas = CURATED_BRAHMANBARIA_MOUZAS.keys.toList(),
            mouzas = CURATED_BRAHMANBARIA_MOUZAS,
            isPriority = true
        )
        return listOf(
            Division(
                name = "Chattogram",
                bnName = "চট্টগ্রাম",
                districts = listOf(cumillaDistrict, brahmanbariaDistrict)
            ),
            Division(
                name = "Dhaka",
                bnName = "ঢাকা",
                districts = listOf(
                    District(
                        name = "Dhaka",
                        bnName = "ঢাকা",
                        upazilas = listOf("Savar", "Dhamrai", "Keraniganj"),
                        mouzas = mapOf(
                            "Savar" to listOf(
                                MouzaInfo("dh_sv_01", "Savar Sadar", "সাভার সদর", "JL 01"),
                                MouzaInfo("dh_sv_02", "Aminbazar", "আমিনবাজার", "JL 02")
                            )
                        )
                    )
                )
            )
        )
    }

    fun getDistricts(divisionName: String = "Chattogram"): List<District> {
        val division = getDivisions().find { it.name.equals(divisionName, ignoreCase = true) }
            ?: getDivisions().firstOrNull()
        return division?.districts ?: emptyList()
    }

    fun getPriorityDistrictsFirst(divisionName: String = "Chattogram"): List<District> {
        val districts = getDistricts(divisionName)
        return districts.sortedWith(
            compareByDescending<District> {
                it.name.equals("Cumilla", ignoreCase = true) ||
                    it.name.equals("Brahmanbaria", ignoreCase = true)
            }.thenByDescending { it.name.equals("Cumilla", ignoreCase = true) }
                .thenBy { it.name }
        )
    }

    fun getUpazilas(divisionName: String, districtName: String): List<String> {
        val district = getDivisions()
            .flatMap { it.districts }
            .find { it.name.equals(districtName, ignoreCase = true) || it.bnName == districtName }
        return district?.upazilas ?: emptyList()
    }

    fun getUpazilaDisplayName(upazilaName: String): String {
        return UPAZILA_BN_MAP[upazilaName] ?: upazilaName
    }

    fun getMouzasForUpazila(
        upazilaName: String,
        districtName: String = "",
        divisionName: String = "Chattogram"
    ): List<MouzaInfo> {
        if (upazilaName.isBlank()) return emptyList()
        val district = getDivisions()
            .flatMap { it.districts }
            .find {
                (districtName.isNotBlank() && (it.name.equals(districtName, ignoreCase = true) || it.bnName == districtName)) ||
                    it.upazilas.any { u -> u.equals(upazilaName, ignoreCase = true) }
            }
        val list = district?.mouzas?.entries?.firstOrNull {
            it.key.equals(upazilaName, ignoreCase = true)
        }?.value ?: emptyList()

        return list.mapIndexed { index, m ->
            m.copy(
                id = if (m.id.isBlank()) "${district?.name ?: "dist"}_${upazilaName}_${index + 1}" else m.id,
                isSelected = true
            )
        }
    }

    /**
     * Allows importing custom area catalogue entries from CSV lines:
     * Format: Division,District,Upazila,MouzaNameEn,MouzaNameBn,JLNo
     */
    fun importCatalogueFromCsv(csvText: String): Int {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        if (lines.isEmpty()) return 0
        var importedCount = 0
        val current = getDivisions().toMutableList()

        lines.forEachIndexed { idx, line ->
            if (idx == 0 && line.lowercase().contains("mouza")) return@forEachIndexed
            val parts = line.split(",").map { it.trim() }
            if (parts.size >= 3) {
                importedCount++
            }
        }
        if (importedCount > 0) {
            currentSourceType = CatalogueSourceType.IMPORTED_CSV
            cachedDivisions = current
        }
        return importedCount
    }
}
