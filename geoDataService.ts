/**
 * Geo Data Service - Land Records Administrative Hierarchy
 * Covers Cumilla (17 Upazilas) and Brahmanbaria (9 Upazilas)
 */

import { District, Upazila, Mouza } from './types';

export const DISTRICTS: District[] = [
  { id: 'cumilla', nameEn: 'Cumilla', nameBn: 'কুমিল্লা' },
  { id: 'brahmanbaria', nameEn: 'Brahmanbaria', nameBn: 'ব্রাহ্মণবাড়িয়া' }
];

export const UPAZILAS: Upazila[] = [
  // Cumilla Upazilas (17)
  { id: 'cumilla_sadar', nameEn: 'Cumilla Adarsha Sadar', nameBn: 'কুমিল্লা আদর্শ সদর', districtId: 'cumilla' },
  { id: 'cumilla_dakshin', nameEn: 'Sadar Dakshin', nameBn: 'সদর দক্ষিণ', districtId: 'cumilla' },
  { id: 'barura', nameEn: 'Barura', nameBn: 'বরুড়া', districtId: 'cumilla' },
  { id: 'brahmanpara', nameEn: 'Brahmanpara', nameBn: 'ব্রাহ্মণপাড়া', districtId: 'cumilla' },
  { id: 'burichang', nameEn: 'Burichang', nameBn: 'বুড়িচং', districtId: 'cumilla' },
  { id: 'chandina', nameEn: 'Chandina', nameBn: 'চান্দিনা', districtId: 'cumilla' },
  { id: 'chauddagram', nameEn: 'Chauddagram', nameBn: 'চৌদ্দগ্রাম', districtId: 'cumilla' },
  { id: 'daudkandi', nameEn: 'Daudkandi', nameBn: 'দাউদকান্দি', districtId: 'cumilla' },
  { id: 'debidwar', nameEn: 'Debidwar', nameBn: 'দেবিদ্বার', districtId: 'cumilla' },
  { id: 'homna', nameEn: 'Homna', nameBn: 'হোমনা', districtId: 'cumilla' },
  { id: 'laksam', nameEn: 'Laksam', nameBn: 'লাকসাম', districtId: 'cumilla' },
  { id: 'lalmai', nameEn: 'Lalmai', nameBn: 'লালমাই', districtId: 'cumilla' },
  { id: 'meghna', nameEn: 'Meghna', nameBn: 'মেঘনা', districtId: 'cumilla' },
  { id: 'monohargonj', nameEn: 'Monohargonj', nameBn: 'মনোহরগঞ্জ', districtId: 'cumilla' },
  { id: 'muradnagar', nameEn: 'Muradnagar', nameBn: 'মুরাদনগর', districtId: 'cumilla' },
  { id: 'nangalkot', nameEn: 'Nangalkot', nameBn: 'নাঙ্গলকোট', districtId: 'cumilla' },
  { id: 'titas', nameEn: 'Titas', nameBn: 'তিতাস', districtId: 'cumilla' },

  // Brahmanbaria Upazilas (9)
  { id: 'brahmanbaria_sadar', nameEn: 'Brahmanbaria Sadar', nameBn: 'ব্রাহ্মণবাড়িয়া সদর', districtId: 'brahmanbaria' },
  { id: 'ashuganj', nameEn: 'Ashuganj', nameBn: 'আশুগঞ্জ', districtId: 'brahmanbaria' },
  { id: 'nasirnagar', nameEn: 'Nasirnagar', nameBn: 'নাসিরনগর', districtId: 'brahmanbaria' },
  { id: 'nabinagar', nameEn: 'Nabinagar', nameBn: 'নবীনগর', districtId: 'brahmanbaria' },
  { id: 'sarail', nameEn: 'Sarail', nameBn: 'সরাইল', districtId: 'brahmanbaria' },
  { id: 'kasba', nameEn: 'Kasba', nameBn: 'কসবা', districtId: 'brahmanbaria' },
  { id: 'akhaura', nameEn: 'Akhaura', nameBn: 'আখাউড়া', districtId: 'brahmanbaria' },
  { id: 'bancharampur', nameEn: 'Bancharampur', nameBn: 'বাঞ্ছারামপুর', districtId: 'brahmanbaria' },
  { id: 'bijoynagar', nameEn: 'Bijoynagar', nameBn: 'বিজয়নগর', districtId: 'brahmanbaria' }
];

// Helper to generate structured mouza records
const generateMouzas = (
  upazilaId: string,
  districtId: string,
  list: { bn: string; en: string; jl: number }[]
): Mouza[] => {
  return list.map((item, idx) => ({
    id: `${upazilaId}_m_${idx + 1}`,
    nameBn: item.bn,
    nameEn: item.en,
    jlNo: `JL ${item.jl.toString().padStart(2, '0')}`,
    upazilaId,
    districtId
  }));
};

// Comprehensive Mouzas dataset for Cumilla and Brahmanbaria
const MOUZA_DATA: Record<string, Mouza[]> = {
  titas: generateMouzas('titas', 'cumilla', [
    { bn: 'সাহাপুর', en: 'Sahapur', jl: 1 },
    { bn: 'বানিগ্রাম', en: 'Banigram', jl: 2 },
    { bn: 'ছাফরা', en: 'Chhafara', jl: 3 },
    { bn: 'ছাফরি', en: 'Chhafari', jl: 4 },
    { bn: 'নালন্ধা', en: 'Nalandha', jl: 5 },
    { bn: 'সাত্তাতাইয়া', en: 'Sattatatiya', jl: 6 },
    { bn: 'মজিদপুর', en: 'Majidpur', jl: 7 },
    { bn: 'রঘুনাথপুর নয়ানি', en: 'Raghunathpur Nayani', jl: 8 },
    { bn: 'আজিমপুর', en: 'Azimpur', jl: 9 },
    { bn: 'দক্ষিণ যুগল শ্রীমাই', en: 'Dakshin Jugal Srimai', jl: 10 },
    { bn: 'কচুয়াই', en: 'Kachuai', jl: 11 },
    { bn: 'কামচাতর', en: 'Kamchator', jl: 12 },
    { bn: 'কথা', en: 'Katha', jl: 13 },
    { bn: 'লট ৭৫ শ্রীমাই', en: 'Lot 75 Srimai', jl: 14 },
    { bn: 'পারিগ্রাম', en: 'Parigram', jl: 15 },
    { bn: 'শ্রীমাই', en: 'Srimai', jl: 16 },
    { bn: 'সাতানী', en: 'Satani', jl: 17 },
    { bn: 'জগতপুর', en: 'Jagatpur', jl: 18 },
    { bn: 'বলরামপুর', en: 'Balorampur', jl: 19 },
    { bn: 'কড়িকান্দি', en: 'Karikandi', jl: 20 },
    { bn: 'কলাকান্দি', en: 'Kalakandi', jl: 21 },
    { bn: 'ভিটিকান্দি', en: 'Vitikandi', jl: 22 },
    { bn: 'নারান্দিয়া', en: 'Narayandia', jl: 23 },
    { bn: 'জিয়ারকান্দি', en: 'Zearkandi', jl: 24 },
    { bn: 'কাঠালিয়া', en: 'Kathalia', jl: 25 },
    { bn: 'পক্ষিশগাতালী', en: 'Pakkhisgatali', jl: 26 },
    { bn: 'রাজদাপুর', en: 'Rajdapur', jl: 27 },
    { bn: 'বিরামকাশি', en: 'Biramkashi', jl: 28 },
    { bn: 'দড়েল্পী', en: 'Darelpi', jl: 29 },
    { bn: 'কারেয়াকান্দি', en: 'Kareyakandi', jl: 30 },
    { bn: 'নোহাপুর', en: 'Nohapur', jl: 31 },
    { bn: 'কালাইয়কাদি', en: 'Kalaiyakadi', jl: 32 },
    { bn: 'মঙ্গলকান্দি', en: 'Mangalkandi', jl: 33 },
    { bn: 'বাতাকান্দি', en: 'Batakandi', jl: 34 },
    { bn: 'গাজীপুর', en: 'Gazipur', jl: 35 },
    { bn: 'গোপালপুর', en: 'Gopalpur', jl: 36 },
    { bn: 'আসাদপুর', en: 'Asadpur', jl: 37 },
    { bn: 'চাঁদেরচর', en: 'Chanderchar', jl: 38 },
    { bn: 'বাহেরচর', en: 'Baherchar', jl: 39 },
    { bn: 'মানিকান্দি', en: 'Manikandi', jl: 40 },
    { bn: 'জয়নগর', en: 'Joynagar', jl: 41 },
    { bn: 'দৌলতপুর', en: 'Daulatpur', jl: 42 },
    { bn: 'মোহনপুর', en: 'Mohanpur', jl: 43 },
    { bn: 'আলমপুর', en: 'Alampur', jl: 44 },
    { bn: 'চর তিতাস', en: 'Char Titas', jl: 45 },
    { bn: 'তিতাস সদর', en: 'Titas Sadar', jl: 46 },
    { bn: 'মাধবপুর', en: 'Madhabpur', jl: 47 },
    { bn: 'সুলতানপুর', en: 'Sultanpur', jl: 48 },
    { bn: 'শ্যামপুর', en: 'Shampur', jl: 49 },
    { bn: 'রামপুর', en: 'Rampur', jl: 50 },
    { bn: 'মির্জাপুর', en: 'Mirzapur', jl: 51 },
    { bn: 'হরিপুর', en: 'Haripur', jl: 52 },
    { bn: 'কৃষ্ণপুর', en: 'Krishnapur', jl: 53 },
    { bn: 'রাধানগর', en: 'Radhanagar', jl: 54 },
    { bn: 'দূর্গাপুর', en: 'Durgapur', jl: 55 },
    { bn: 'গোবিন্দপুর', en: 'Gobindapur', jl: 56 },
    { bn: 'ফতেহাবাদ', en: 'Fatehabad', jl: 57 },
    { bn: 'ভবানীপুর', en: 'Bhabanipur', jl: 58 },
    { bn: 'কামালপুর', en: 'Kamalpur', jl: 59 },
    { bn: 'সোনাপুর', en: 'Sonapur', jl: 60 },
    { bn: 'উত্তর তিতাস', en: 'Uttar Titas', jl: 61 }
  ]),

  cumilla_sadar: generateMouzas('cumilla_sadar', 'cumilla', [
    { bn: 'চকবাজার', en: 'Chawkbazar', jl: 1 },
    { bn: 'বাগিচাগাঁও', en: 'Bagichagaon', jl: 2 },
    { bn: 'ঝাউতলা', en: 'Jhautala', jl: 3 },
    { bn: 'শাসনগাছা', en: 'Shasangacha', jl: 4 },
    { bn: 'ধর্মপুর', en: 'Dharmapur', jl: 5 },
    { bn: 'টমছম ব্রিজ', en: 'Tomsom Bridge', jl: 6 },
    { bn: 'কালিয়াজুরি', en: 'Kaliajuri', jl: 7 },
    { bn: 'ছোটরা', en: 'Chhotra', jl: 8 },
    { bn: 'কান্দিরপাড়', en: 'Kandirpar', jl: 9 },
    { bn: 'বাদুরতলা', en: 'Badurtala', jl: 10 },
    { bn: 'মোগলতলী', en: 'Moghaltoli', jl: 11 },
    { bn: 'আমড়াতলী', en: 'Amratali', jl: 12 },
    { bn: 'পাঁচথুবী', en: 'Panchthubi', jl: 13 },
    { bn: 'জগন্নাথপুর', en: 'Jagannathpur', jl: 14 },
    { bn: 'দুর্গাপুর উত্তর', en: 'Durgapur North', jl: 15 },
    { bn: 'দুর্গাপুর দক্ষিণ', en: 'Durgapur South', jl: 16 }
  ]),

  cumilla_dakshin: generateMouzas('cumilla_dakshin', 'cumilla', [
    { bn: 'বিজয়পুর', en: 'Bijoypur', jl: 1 },
    { bn: 'চৌয়ারা', en: 'Chowara', jl: 2 },
    { bn: 'গলিয়ারা', en: 'Galiara', jl: 3 },
    { bn: 'বারপাড়া', en: 'Barapara', jl: 4 },
    { bn: 'জোড়কানন পূর্ব', en: 'Jorkanon East', jl: 5 },
    { bn: 'জোড়কানন পশ্চিম', en: 'Jorkanon West', jl: 6 },
    { bn: 'বেলতলী', en: 'Beltali', jl: 7 },
    { bn: 'বাগমারা', en: 'Bagmara', jl: 8 },
    { bn: 'পেরুল উত্তর', en: 'Perul North', jl: 9 },
    { bn: 'পেরুল দক্ষিণ', en: 'Perul South', jl: 10 }
  ]),

  barura: generateMouzas('barura', 'cumilla', [
    { bn: 'বরুড়া সদর', en: 'Barura Sadar', jl: 1 },
    { bn: 'পয়ালগাছা', en: 'Payalgacha', jl: 2 },
    { bn: 'ঝলম', en: 'Jhalam', jl: 3 },
    { bn: 'চিতড্ডা', en: 'Chitadda', jl: 4 },
    { bn: 'ভবানিপুর', en: 'Bhabanipur', jl: 5 },
    { bn: 'খোশবাস', en: 'Khoshbas', jl: 6 },
    { bn: 'আড্ডা', en: 'Adda', jl: 7 },
    { bn: 'আদ্রা', en: 'Adra', jl: 8 },
    { bn: 'গালিমপুর', en: 'Galimpur', jl: 9 },
    { bn: 'শাকপুর', en: 'Shakpur', jl: 10 }
  ]),

  brahmanpara: generateMouzas('brahmanpara', 'cumilla', [
    { bn: 'মালাপাড়া', en: 'Malapara', jl: 1 },
    { bn: 'সাহেবাবাদ', en: 'Sahebabad', jl: 2 },
    { bn: 'দুলালপুর', en: 'Dulalpur', jl: 3 },
    { bn: 'শশীদল', en: 'Shashidal', jl: 4 },
    { bn: 'চান্দলা', en: 'Chandla', jl: 5 },
    { bn: 'শিদলাই', en: 'Shidlai', jl: 6 },
    { bn: 'মাধবপুর', en: 'Madhabpur', jl: 7 },
    { bn: 'ব্রাহ্মণপাড়া সদর', en: 'Brahmanpara Sadar', jl: 8 }
  ]),

  burichang: generateMouzas('burichang', 'cumilla', [
    { bn: 'বুড়িচং সদর', en: 'Burichang Sadar', jl: 1 },
    { bn: 'বাকশীমূল', en: 'Bakshimul', jl: 2 },
    { bn: 'পীরযাত্রাপুর', en: 'Pirjatrapur', jl: 3 },
    { bn: 'ষোলনল', en: 'Sholonal', jl: 4 },
    { bn: 'রাজাপুর', en: 'Rajapur', jl: 5 },
    { bn: 'ময়নামতি', en: 'Mainamati', jl: 6 },
    { bn: 'মোকাম', en: 'Mokam', jl: 7 },
    { bn: 'ভারেল্লা', en: 'Bharella', jl: 8 }
  ]),

  chandina: generateMouzas('chandina', 'cumilla', [
    { bn: 'চান্দিনা পৌরসভা', en: 'Chandina Pouroshova', jl: 1 },
    { bn: 'সুহিলপুর', en: 'Suhilpur', jl: 2 },
    { bn: 'বাতাগাছি', en: 'Batagachi', jl: 3 },
    { bn: 'জোয়াগ', en: 'Joyag', jl: 4 },
    { bn: 'বরকইট', en: 'Borkoit', jl: 5 },
    { bn: 'মাইজখার', en: 'Maijkhar', jl: 6 },
    { bn: 'গল্লাই', en: 'Gallai', jl: 7 },
    { bn: 'দোল্লাই', en: 'Dollai', jl: 8 },
    { bn: 'কেরণখাল', en: 'Keronkhal', jl: 9 },
    { bn: 'মহিচাইল', en: 'Mohichail', jl: 10 }
  ]),

  chauddagram: generateMouzas('chauddagram', 'cumilla', [
    { bn: 'কাশিনগর', en: 'Kashinagar', jl: 1 },
    { bn: 'উজিরপুর', en: 'Ujirpur', jl: 2 },
    { bn: 'কালিকাপুর', en: 'Kalikapur', jl: 3 },
    { bn: 'শ্রীপুর', en: 'Sreepur', jl: 4 },
    { bn: 'শুভপুর', en: 'Shubhapur', jl: 5 },
    { bn: 'ঘোলপাশা', en: 'Gholpasha', jl: 6 },
    { bn: 'মুন্সিরহাট', en: 'Munsirhat', jl: 7 },
    { bn: 'বাতিসা', en: 'Batisa', jl: 8 },
    { bn: 'কনকাপৈত', en: 'Konkapoit', jl: 9 },
    { bn: 'চিওড়া', en: 'Chiora', jl: 10 },
    { bn: 'গুনবতী', en: 'Gunabati', jl: 11 },
    { bn: 'জগন্নাথদিঘী', en: 'Jagannathdighi', jl: 12 },
    { bn: 'আলকরা', en: 'Alkara', jl: 13 }
  ]),

  daudkandi: generateMouzas('daudkandi', 'cumilla', [
    { bn: 'দাউদকান্দি সদর', en: 'Daudkandi Sadar', jl: 1 },
    { bn: 'গৌরীপুর', en: 'Gouripur', jl: 2 },
    { bn: 'সুন্দলপুর', en: 'Sundalpur', jl: 3 },
    { bn: 'বারপাড়া', en: 'Barapara', jl: 4 },
    { bn: 'মারুকা', en: 'Maruka', jl: 5 },
    { bn: 'মোহাম্মদপুর', en: 'Mohammadpur', jl: 6 },
    { bn: 'পাঁচগাছিয়া', en: 'Panchgachia', jl: 7 },
    { bn: 'ইলিয়টগঞ্জ', en: 'Eliotganj', jl: 8 },
    { bn: 'জিংলাতলী', en: 'Jinglatoli', jl: 9 },
    { bn: 'বিটেশ্বর', en: 'Biteshwar', jl: 10 }
  ]),

  debidwar: generateMouzas('debidwar', 'cumilla', [
    { bn: 'দেবিদ্বার সদর', en: 'Debidwar Sadar', jl: 1 },
    { bn: 'বড়শালঘর', en: 'Barashalghar', jl: 2 },
    { bn: 'ইউসুফপুর', en: 'Yousufpur', jl: 3 },
    { bn: 'রসুলপুর', en: 'Rasulpur', jl: 4 },
    { bn: 'সুবিদপুর', en: 'Subidpur', jl: 5 },
    { bn: 'ফতেহাবাদ', en: 'Fatehabad', jl: 6 },
    { bn: 'এলাহাবাদ', en: 'Elahabad', jl: 7 },
    { bn: 'জাফরগঞ্জ', en: 'Jafarganj', jl: 8 },
    { bn: 'গুনাইঘর', en: 'Gunaighar', jl: 9 },
    { bn: 'ধামতী', en: 'Dhamti', jl: 10 }
  ]),

  homna: generateMouzas('homna', 'cumilla', [
    { bn: 'হোমনা সদর', en: 'Homna Sadar', jl: 1 },
    { bn: 'মাথাভাঙ্গা', en: 'Mathabhanga', jl: 2 },
    { bn: 'ঘাগুটিয়া', en: 'Ghagutia', jl: 3 },
    { bn: 'দুলালপুর', en: 'Dulalpur', jl: 4 },
    { bn: 'চান্দেরচর', en: 'Chanderchar', jl: 5 },
    { bn: 'আসাদপুর', en: 'Asadpur', jl: 6 },
    { bn: 'নিলখী', en: 'Nilokhi', jl: 7 },
    { bn: 'ভাসানিয়া', en: 'Bhasania', jl: 8 }
  ]),

  laksam: generateMouzas('laksam', 'cumilla', [
    { bn: 'লাকসাম সদর', en: 'Laksam Sadar', jl: 1 },
    { bn: 'বাকই', en: 'Bakoi', jl: 2 },
    { bn: 'মুদাফরগঞ্জ', en: 'Mudafarganj', jl: 3 },
    { bn: 'কান্দিরপাড়', en: 'Kandirpar', jl: 4 },
    { bn: 'গোবিন্দপুর', en: 'Gobindapur', jl: 5 },
    { bn: 'উত্তরদা', en: 'Uttarda', jl: 6 },
    { bn: 'আজগরা', en: 'Azgara', jl: 7 }
  ]),

  lalmai: generateMouzas('lalmai', 'cumilla', [
    { bn: 'বাগমারা উত্তর', en: 'Bagmara North', jl: 1 },
    { bn: 'বাগমারা দক্ষিণ', en: 'Bagmara South', jl: 2 },
    { bn: 'ভুলইন উত্তর', en: 'Bhuloin North', jl: 3 },
    { bn: 'ভুলইন দক্ষিণ', en: 'Bhuloin South', jl: 4 },
    { bn: 'পেরুল উত্তর', en: 'Perul North', jl: 5 },
    { bn: 'পেরুল দক্ষিণ', en: 'Perul South', jl: 6 },
    { bn: 'বেলঘর উত্তর', en: 'Belghar North', jl: 7 },
    { bn: 'বেলঘর দক্ষিণ', en: 'Belghar South', jl: 8 }
  ]),

  meghna: generateMouzas('meghna', 'cumilla', [
    { bn: 'মানিকারচর', en: 'Manikarchar', jl: 1 },
    { bn: 'চন্দনপুর', en: 'Chandanpur', jl: 2 },
    { bn: 'চালিবাঙ্গা', en: 'Chalibanga', jl: 3 },
    { bn: 'গোবিন্দপুর', en: 'Gobindapur', jl: 4 },
    { bn: 'ভাওরখোলা', en: 'Bhaorkhola', jl: 5 },
    { bn: 'রাধানগর', en: 'Radhanagar', jl: 6 },
    { bn: 'লুটেরচর', en: 'Luterchar', jl: 7 }
  ]),

  monohargonj: generateMouzas('monohargonj', 'cumilla', [
    { bn: 'মনোহরগঞ্জ সদর', en: 'Monohargonj Sadar', jl: 1 },
    { bn: 'বাইশগাঁও', en: 'Baishgaon', jl: 2 },
    { bn: 'সরসপুর', en: 'Saraspur', jl: 3 },
    { bn: 'হাসনাবাদ', en: 'Hasnabad', jl: 4 },
    { bn: 'ঝলম উত্তর', en: 'Jhalam North', jl: 5 },
    { bn: 'ঝলম দক্ষিণ', en: 'Jhalam South', jl: 6 },
    { bn: 'মৈশাতুয়া', en: 'Moishatua', jl: 7 },
    { bn: 'লক্ষণপুর', en: 'Lakshmanpur', jl: 8 },
    { bn: 'খিলা', en: 'Khila', jl: 9 },
    { bn: 'উত্তর হাওলা', en: 'Uttar Hawla', jl: 10 }
  ]),

  muradnagar: generateMouzas('muradnagar', 'cumilla', [
    { bn: 'মুরাদনগর সদর', en: 'Muradnagar Sadar', jl: 1 },
    { bn: 'শ্রীকাইল', en: 'Srikail', jl: 2 },
    { bn: 'আকুবপুর', en: 'Akubpur', jl: 3 },
    { bn: 'আন্দিকোট', en: 'Andikot', jl: 4 },
    { bn: 'পূর্বধইর পূর্ব', en: 'Purbadhair East', jl: 5 },
    { bn: 'পূর্বধইর পশ্চিম', en: 'Purbadhair West', jl: 6 },
    { bn: 'রামচন্দ্রপুর উত্তর', en: 'Ramchandrapur North', jl: 7 },
    { bn: 'রামচন্দ্রপুর দক্ষিণ', en: 'Ramchandrapur South', jl: 8 },
    { bn: 'বাঙ্গরা পূর্ব', en: 'Bangra East', jl: 9 },
    { bn: 'বাঙ্গরা পশ্চিম', en: 'Bangra West', jl: 10 },
    { bn: 'ধামঘর', en: 'Dhamghar', jl: 11 },
    { bn: 'জাহাপুর', en: 'Jahapur', jl: 12 },
    { bn: 'দারোরা', en: 'Darora', jl: 13 }
  ]),

  nangalkot: generateMouzas('nangalkot', 'cumilla', [
    { bn: 'নাঙ্গলকোট পৌরসভা', en: 'Nangalkot Pouroshova', jl: 1 },
    { bn: 'বাঙ্গড্ডা', en: 'Bangadda', jl: 2 },
    { bn: 'পেড়িয়া', en: 'Peria', jl: 3 },
    { bn: 'রায়কোট উত্তর', en: 'Raykot North', jl: 4 },
    { bn: 'রায়কোট দক্ষিণ', en: 'Raykot South', jl: 5 },
    { bn: 'মোকরা', en: 'Mokra', jl: 6 },
    { bn: 'মক্রবপুর', en: 'Makrabpur', jl: 7 },
    { bn: 'হেসাখাল', en: 'Hesakhal', jl: 8 },
    { bn: 'বক্সগঞ্জ', en: 'Bakshaganj', jl: 9 },
    { bn: 'ঢালুয়া', en: 'Dhalua', jl: 10 },
    { bn: 'দৌলখাঁড়', en: 'Doulkhar', jl: 11 },
    { bn: 'জোড্ডা', en: 'Jodda', jl: 12 }
  ]),

  // Brahmanbaria Mouzas (9 Upazilas)
  brahmanbaria_sadar: generateMouzas('brahmanbaria_sadar', 'brahmanbaria', [
    { bn: 'মেদ্দা', en: 'Medda', jl: 1 },
    { bn: 'পৈরতলা', en: 'Poirtala', jl: 2 },
    { bn: 'কাউতলী', en: 'Kawtoli', jl: 3 },
    { bn: 'মৌড়াইল', en: 'Mowrail', jl: 4 },
    { bn: 'গোকর্ণঘাট', en: 'Gokarnaghat', jl: 5 },
    { bn: 'সুহিলপুর', en: 'Suhilpur', jl: 6 },
    { bn: 'বুধল', en: 'Budhal', jl: 7 },
    { bn: 'তালশহর পূর্ব', en: 'Talshahar East', jl: 8 },
    { bn: 'নাটাই উত্তর', en: 'Natai North', jl: 9 },
    { bn: 'নাটাই দক্ষিণ', en: 'Natai South', jl: 10 },
    { bn: 'বাসুদেব', en: 'Basudeb', jl: 11 },
    { bn: 'মাছিহাতা', en: 'Machihata', jl: 12 },
    { bn: 'সুলতানপুর', en: 'Sultanpur', jl: 13 }
  ]),

  ashuganj: generateMouzas('ashuganj', 'brahmanbaria', [
    { bn: 'আশুগঞ্জ সদর', en: 'Ashuganj Sadar', jl: 1 },
    { bn: 'চর চারতলা', en: 'Char Chartala', jl: 2 },
    { bn: 'দুর্গাপুর', en: 'Durgapur', jl: 3 },
    { bn: 'তালশহর পশ্চিম', en: 'Talshahar West', jl: 4 },
    { bn: 'আড়াইসিধা', en: 'Araisidha', jl: 5 },
    { bn: 'শরীফপুর', en: 'Sharifpur', jl: 6 },
    { bn: 'লালপুর', en: 'Lalpur', jl: 7 },
    { bn: 'তারুয়া', en: 'Tarua', jl: 8 }
  ]),

  nasirnagar: generateMouzas('nasirnagar', 'brahmanbaria', [
    { bn: 'নাসিরনগর সদর', en: 'Nasirnagar Sadar', jl: 1 },
    { bn: 'চাতলপাড়', en: 'Chatalpar', jl: 2 },
    { bn: 'ভলাকূট', en: 'Bhalakut', jl: 3 },
    { bn: 'গোয়ালনগর', en: 'Goalnagar', jl: 4 },
    { bn: 'কুন্দা', en: 'Kunda', jl: 5 },
    { bn: 'ফান্দাউক', en: 'Phandauk', jl: 6 },
    { bn: 'বুড়িশ্বর', en: 'Burishwar', jl: 7 },
    { bn: 'গোকর্ণ', en: 'Gokarna', jl: 8 },
    { bn: 'হরিপুর', en: 'Haripur', jl: 9 },
    { bn: 'পূর্বভাগ', en: 'Purbabhag', jl: 10 }
  ]),

  nabinagar: generateMouzas('nabinagar', 'brahmanbaria', [
    { bn: 'নবীনগর পৌরসভা', en: 'Nabinagar Pouroshova', jl: 1 },
    { bn: 'বড়াইল', en: 'Barail', jl: 2 },
    { bn: 'বীরগাঁও', en: 'Birgaon', jl: 3 },
    { bn: 'কৃষ্ণনগর', en: 'Krishnanagar', jl: 4 },
    { bn: 'নাটঘর', en: 'Natghar', jl: 5 },
    { bn: 'বিদ্যাকুট', en: 'Bidyakut', jl: 6 },
    { bn: 'কাইতলা উত্তর', en: 'Kaitola North', jl: 7 },
    { bn: 'কাইতলা দক্ষিণ', en: 'Kaitola South', jl: 8 },
    { bn: 'বিটঘর', en: 'Bitghar', jl: 9 },
    { bn: 'শিবপুর', en: 'Shibpur', jl: 10 },
    { bn: 'শ্রীরামপুর', en: 'Sreerampur', jl: 11 },
    { bn: 'জিনোদপুর', en: 'Jinodpur', jl: 12 },
    { bn: 'রসুল্লাবাদ', en: 'Rasullabad', jl: 13 },
    { bn: 'শ্যামগ্রাম', en: 'Shyamgram', jl: 14 }
  ]),

  sarail: generateMouzas('sarail', 'brahmanbaria', [
    { bn: 'সরাইল সদর', en: 'Sarail Sadar', jl: 1 },
    { bn: 'অরুয়াইল', en: 'Aruail', jl: 2 },
    { bn: 'পাকশিমুল', en: 'Pakshimul', jl: 3 },
    { bn: 'কালীকচ্ছ', en: 'Kalikachha', jl: 4 },
    { bn: 'চুন্টা', en: 'Chunta', jl: 5 },
    { bn: 'শাহবাজপুর', en: 'Shahbazpur', jl: 6 },
    { bn: 'শাহজাদাপুর', en: 'Shahjadapur', jl: 7 },
    { bn: 'নোয়াগাঁও', en: 'Noagaon', jl: 8 },
    { bn: 'পানিশ্বর', en: 'Panishwar', jl: 9 }
  ]),

  kasba: generateMouzas('kasba', 'brahmanbaria', [
    { bn: 'কসবা পৌরসভা', en: 'Kasba Pouroshova', jl: 1 },
    { bn: 'মূলগ্রাম', en: 'Moolgram', jl: 2 },
    { bn: 'মেহারী', en: 'Mehari', jl: 3 },
    { bn: 'বাদৈর', en: 'Badair', jl: 4 },
    { bn: 'খাড়েরা', en: 'Kharera', jl: 5 },
    { bn: 'বিনাউটি', en: 'Binauti', jl: 6 },
    { bn: 'গোপীনাথপুর', en: 'Gopinathpur', jl: 7 },
    { bn: 'কুটি', en: 'Kuti', jl: 8 },
    { bn: 'কায়েমপুর', en: 'Kayempur', jl: 9 },
    { bn: 'বাইয়াক', en: 'Bayek', jl: 10 }
  ]),

  akhaura: generateMouzas('akhaura', 'brahmanbaria', [
    { bn: 'আখাউড়া সদর', en: 'Akhaura Sadar', jl: 1 },
    { bn: 'মোগড়া', en: 'Mogra', jl: 2 },
    { bn: 'ধরখার', en: 'Dharkhar', jl: 3 },
    { bn: 'মোনিয়ন্দ', en: 'Moniand', jl: 4 },
    { bn: 'গঙ্গাসাগর', en: 'Gangasagar', jl: 5 },
    { bn: 'তারেকপুর', en: 'Tarekpur', jl: 6 },
    { bn: 'দেবগ্রাম', en: 'Debgram', jl: 7 }
  ]),

  bancharampur: generateMouzas('bancharampur', 'brahmanbaria', [
    { bn: 'বাঞ্ছারামপুর সদর', en: 'Bancharampur Sadar', jl: 1 },
    { bn: 'তেজখালী', en: 'Tejkhali', jl: 2 },
    { bn: 'পাহাড়িয়াকান্দি', en: 'Pahariakandi', jl: 3 },
    { bn: 'দরিয়াদৌলত', en: 'Dariyadoulat', jl: 4 },
    { bn: 'সোনারামপুর', en: 'Sonarampur', jl: 5 },
    { bn: 'ছলিমাবাদ', en: 'Chhalimabad', jl: 6 },
    { bn: 'উজানচর', en: 'Ujanchar', jl: 7 },
    { bn: 'মানিকপুর', en: 'Manikpur', jl: 8 },
    { bn: 'ফরদাবাদ', en: 'Fardabad', jl: 9 },
    { bn: 'রূপসদী', en: 'Rupsadi', jl: 10 }
  ]),

  bijoynagar: generateMouzas('bijoynagar', 'brahmanbaria', [
    { bn: 'বুধন্তী', en: 'Budhanti', jl: 1 },
    { bn: 'চান্দুরা', en: 'Chandura', jl: 2 },
    { bn: 'ইছাপুরা', en: 'Ichhapura', jl: 3 },
    { bn: 'চম্পকনগর', en: 'Champaknagar', jl: 4 },
    { bn: 'হরষপুর', en: 'Harashpur', jl: 5 },
    { bn: 'পত্তন', en: 'Pattan', jl: 6 },
    { bn: 'সিঙ্গারবিল', en: 'Singarbil', jl: 7 },
    { bn: 'বিষ্ণুপুর', en: 'Bishnupur', jl: 8 },
    { bn: 'চর ইসলামপুর', en: 'Char Islampur', jl: 9 },
    { bn: 'পাহাড়পুর', en: 'Paharpur', jl: 10 }
  ])
};

export class GeoDataService {
  static getDistricts(): District[] {
    return DISTRICTS;
  }

  static getDistrictById(districtId: string): District | undefined {
    return DISTRICTS.find(d => d.id === districtId);
  }

  static getUpazilasByDistrict(districtId: string): Upazila[] {
    return UPAZILAS.filter(u => u.districtId === districtId);
  }

  static getUpazilaById(upazilaId: string): Upazila | undefined {
    return UPAZILAS.find(u => u.id === upazilaId);
  }

  static getMouzasByUpazila(upazilaId: string): Mouza[] {
    return MOUZA_DATA[upazilaId] || [];
  }

  static getMouzaCountForUpazila(upazilaId: string): number {
    return (MOUZA_DATA[upazilaId] || []).length;
  }
}
