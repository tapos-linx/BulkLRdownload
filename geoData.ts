// geoData.ts
import { District } from './types';

export const GEO_DATA: District[] = [
  {
    id: 'brahmanbaria',
    nameBn: 'ব্রাহ্মণবাড়িয়া',
    upazilas: [
      {
        id: 'bb_sadar',
        nameBn: 'ব্রাহ্মণবাড়িয়া সদর',
        mouzas: [
          { id: 'bb_s_01', nameBn: 'মেদ্দা', jlNo: '০১' },
          { id: 'bb_s_02', nameBn: 'পাইকপাড়া', jlNo: '০২' },
          { id: 'bb_s_03', nameBn: 'পুনিয়াউট', jlNo: '০৩' },
          { id: 'bb_s_04', nameBn: 'কাজীপাড়া', jlNo: '০৪' },
          { id: 'bb_s_05', nameBn: 'ঘাটুরা', jlNo: '০৫' },
          { id: 'bb_s_06', nameBn: 'সুহিলপুর', jlNo: '০৬' },
          { id: 'bb_s_07', nameBn: 'নাটাই', jlNo: '০৭' },
          { id: 'bb_s_08', nameBn: 'বিরামপুর', jlNo: '০৮' }
        ]
      },
      {
        id: 'bb_sarail',
        nameBn: 'সরাইল',
        mouzas: [
          { id: 'bb_sr_01', nameBn: 'সরাইল', jlNo: '১০' },
          { id: 'bb_sr_02', nameBn: 'অন্নদাউট', jlNo: '১১' },
          { id: 'bb_sr_03', nameBn: 'কালিকচ্ছ', jlNo: '১২' },
          { id: 'bb_sr_04', nameBn: 'পানিশ্বর', jlNo: '১৩' },
          { id: 'bb_sr_05', nameBn: 'শাহবাজপুর', jlNo: '১৪' }
        ]
      },
      {
        id: 'bb_ashuganj',
        nameBn: 'আশুগঞ্জ',
        mouzas: [
          { id: 'bb_as_01', nameBn: 'আশুগঞ্জ', jlNo: '২০' },
          { id: 'bb_as_02', nameBn: 'চর চারতলা', jlNo: '২১' },
          { id: 'bb_as_03', nameBn: 'তারুয়া', jlNo: '২২' },
          { id: 'bb_as_04', nameBn: 'আড়াইসিধা', jlNo: '২৩' }
        ]
      },
      {
        id: 'bb_kasba',
        nameBn: 'কসবা',
        mouzas: [
          { id: 'bb_kb_01', nameBn: 'কসবা', jlNo: '৩০' },
          { id: 'bb_kb_02', nameBn: 'কুটি', jlNo: '৩১' },
          { id: 'bb_kb_03', nameBn: 'বিনাউটি', jlNo: '৩২' },
          { id: 'bb_kb_04', nameBn: 'মেহারী', jlNo: '৩৩' }
        ]
      },
      {
        id: 'bb_nabinagar',
        nameBn: 'নবীনগর',
        mouzas: [
          { id: 'bb_nn_01', nameBn: 'নবীনগর', jlNo: '৪০' },
          { id: 'bb_nn_02', nameBn: 'রসুল্লাবাদ', jlNo: '৪১' },
          { id: 'bb_nn_03', nameBn: 'শিবপুর', jlNo: '৪২' },
          { id: 'bb_nn_04', nameBn: 'শ্রীরামপুর', jlNo: '৪৩' }
        ]
      },
      {
        id: 'bb_nasirnagar',
        nameBn: 'নাসিরনগর',
        mouzas: [
          { id: 'bb_ns_01', nameBn: 'নাসিরনগর', jlNo: '৫০' },
          { id: 'bb_ns_02', nameBn: 'চাতলপাড়', jlNo: '৫১' },
          { id: 'bb_ns_03', nameBn: 'গোয়ালনগর', jlNo: '৫২' }
        ]
      },
      {
        id: 'bb_bancharampur',
        nameBn: 'বাঞ্ছারামপুর',
        mouzas: [
          { id: 'bb_bc_01', nameBn: 'বাঞ্ছারামপুর', jlNo: '৬০' },
          { id: 'bb_bc_02', nameBn: 'উজানচর', jlNo: '৬১' },
          { id: 'bb_bc_03', nameBn: 'দড়িকান্দি', jlNo: '৬২' }
        ]
      },
      {
        id: 'bb_akhaura',
        nameBn: 'আখাউড়া',
        mouzas: [
          { id: 'bb_ak_01', nameBn: 'আখাউড়া', jlNo: '৭০' },
          { id: 'bb_ak_02', nameBn: 'মোগড়া', jlNo: '৭১' },
          { id: 'bb_ak_03', nameBn: 'ধরখার', jlNo: '৭২' }
        ]
      },
      {
        id: 'bb_bijoynagar',
        nameBn: 'বিজয়নগর',
        mouzas: [
          { id: 'bb_bj_01', nameBn: 'চম্পকনগর', jlNo: '৮০' },
          { id: 'bb_bj_02', nameBn: 'হরষপুর', jlNo: '৮১' },
          { id: 'bb_bj_03', nameBn: 'সিঙ্গারবিল', jlNo: '৮২' }
        ]
      }
    ]
  },
  {
    id: 'cumilla',
    nameBn: 'কুমিল্লা',
    upazilas: [
      {
        id: 'cm_sadar',
        nameBn: 'আদর্শ সদর',
        mouzas: [
          { id: 'cm_as_01', nameBn: 'কালিকাপুর', jlNo: '১০১' },
          { id: 'cm_as_02', nameBn: 'বাগিচাগাঁও', jlNo: '১০২' },
          { id: 'cm_as_03', nameBn: 'ছোটরা', jlNo: '১০৩' },
          { id: 'cm_as_04', nameBn: 'শাসনগাছা', jlNo: '১০৪' },
          { id: 'cm_as_05', nameBn: 'আমড়াতলী', jlNo: '১০৫' },
          { id: 'cm_as_06', nameBn: 'জগন্নাথপুর', jlNo: '১০৬' }
        ]
      },
      {
        id: 'cm_sadar_dakshin',
        nameBn: 'সদর দক্ষিণ',
        mouzas: [
          { id: 'cm_sd_01', nameBn: 'বিজয়পুর', jlNo: '১১০' },
          { id: 'cm_sd_02', nameBn: 'চৌয়ারা', jlNo: '১১১' },
          { id: 'cm_sd_03', nameBn: 'গলিয়ারা', jlNo: '১১২' }
        ]
      },
      {
        id: 'cm_chandina',
        nameBn: 'চান্দিনা',
        mouzas: [
          { id: 'cm_cd_01', nameBn: 'চান্দিনা', jlNo: '১২০' },
          { id: 'cm_cd_02', nameBn: 'মহিচাইল', jlNo: '১২১' },
          { id: 'cm_cd_03', nameBn: 'কেরণখাল', jlNo: '১২২' }
        ]
      },
      {
        id: 'cm_daudkandi',
        nameBn: 'দাউদকান্দি',
        mouzas: [
          { id: 'cm_dk_01', nameBn: 'দাউদকান্দি', jlNo: '১৩০' },
          { id: 'cm_dk_02', nameBn: 'গৌরীপুর', jlNo: '১৩১' },
          { id: 'cm_dk_03', nameBn: 'ইলিয়টগঞ্জ', jlNo: '১৩২' }
        ]
      },
      {
        id: 'cm_debidwar',
        nameBn: 'দেবিদ্বার',
        mouzas: [
          { id: 'cm_db_01', nameBn: 'দেবিদ্বার', jlNo: '১৪০' },
          { id: 'cm_db_02', nameBn: 'গুনাইঘর', jlNo: '১৪১' },
          { id: 'cm_db_03', nameBn: 'মোহনপুর', jlNo: '১৪২' }
        ]
      },
      {
        id: 'cm_burichang',
        nameBn: 'বুড়িচং',
        mouzas: [
          { id: 'cm_br_01', nameBn: 'বুড়িচং', jlNo: '১৫০' },
          { id: 'cm_br_02', nameBn: 'ময়নামতি', jlNo: '১৫১' },
          { id: 'cm_br_03', nameBn: 'রাজাপুর', jlNo: '১৫২' }
        ]
      },
      {
        id: 'cm_brahmanpara',
        nameBn: 'ব্রাহ্মণপাড়া',
        mouzas: [
          { id: 'cm_bp_01', nameBn: 'ব্রাহ্মণপাড়া', jlNo: '১৬০' },
          { id: 'cm_bp_02', nameBn: 'মাধবপুর', jlNo: '১৬১' },
          { id: 'cm_bp_03', nameBn: 'শশীদল', jlNo: '১৬২' }
        ]
      },
      {
        id: 'cm_chauddagram',
        nameBn: 'চৌদ্দগ্রাম',
        mouzas: [
          { id: 'cm_cg_01', nameBn: 'চৌদ্দগ্রাম', jlNo: '১৭০' },
          { id: 'cm_cg_02', nameBn: 'চিওড়া', jlNo: '১৭১' },
          { id: 'cm_cg_03', nameBn: 'গুণবতী', jlNo: '১৭২' }
        ]
      },
      {
        id: 'cm_laksam',
        nameBn: 'লাকসাম',
        mouzas: [
          { id: 'cm_lk_01', nameBn: 'লাকসাম', jlNo: '১৮০' },
          { id: 'cm_lk_02', nameBn: 'মুদাফরগঞ্জ', jlNo: '১৮১' },
          { id: 'cm_lk_03', nameBn: 'কান্দিরপাড়', jlNo: '১৮২' }
        ]
      },
      {
        id: 'cm_muradnagar',
        nameBn: 'মুরাদনগর',
        mouzas: [
          { id: 'cm_mr_01', nameBn: 'মুরাদনগর', jlNo: '১৯০' },
          { id: 'cm_mr_02', nameBn: 'ধামঘর', jlNo: '১৯১' },
          { id: 'cm_mr_03', nameBn: 'জাহাপুর', jlNo: '১৯২' }
        ]
      },
      {
        id: 'cm_barura',
        nameBn: 'বরুড়া',
        mouzas: [
          { id: 'cm_ba_01', nameBn: 'বরুড়া', jlNo: '২০০' },
          { id: 'cm_ba_02', nameBn: 'পয়ালগাছা', jlNo: '২০১' }
        ]
      },
      {
        id: 'cm_homna',
        nameBn: 'হোমনা',
        mouzas: [
          { id: 'cm_hm_01', nameBn: 'হোমনা', jlNo: '২১০' },
          { id: 'cm_hm_02', nameBn: 'ঘাগুটিয়া', jlNo: '২১১' }
        ]
      },
      {
        id: 'cm_titas',
        nameBn: 'তিতাস',
        mouzas: [
          { id: 'cm_tt_01', nameBn: 'কড়িকান্দি', jlNo: '২২০' },
          { id: 'cm_tt_02', nameBn: 'মজিদপুর', jlNo: '২২১' }
        ]
      },
      {
        id: 'cm_meghna',
        nameBn: 'মেঘনা',
        mouzas: [
          { id: 'cm_mg_01', nameBn: 'মানিকারচর', jlNo: '২৩০' },
          { id: 'cm_mg_02', nameBn: 'গোবিন্দপুর', jlNo: '২৩১' }
        ]
      },
      {
        id: 'cm_monohargonj',
        nameBn: 'মনোহরগঞ্জ',
        mouzas: [
          { id: 'cm_mn_01', nameBn: 'মনোহরগঞ্জ', jlNo: '২৪০' },
          { id: 'cm_mn_02', nameBn: 'বাইশগাঁও', jlNo: '২৪১' }
        ]
      },
      {
        id: 'cm_nangalkot',
        nameBn: 'নাঙ্গলকোট',
        mouzas: [
          { id: 'cm_nk_01', nameBn: 'নাঙ্গলকোট', jlNo: '২৫০' },
          { id: 'cm_nk_02', nameBn: 'রায়কোট', jlNo: '২৫১' }
        ]
      },
      {
        id: 'cm_lalmai',
        nameBn: 'লালমাই',
        mouzas: [
          { id: 'cm_lm_01', nameBn: 'বাগমারা', jlNo: '২৬০' },
          { id: 'cm_lm_02', nameBn: 'পেরুল', jlNo: '২৬১' }
        ]
      }
    ]
  }
];
