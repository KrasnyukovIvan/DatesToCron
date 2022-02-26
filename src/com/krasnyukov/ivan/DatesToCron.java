package com.krasnyukov.ivan;

import com.digdes.school.DatesToCronConvertException;
import com.digdes.school.DatesToCronConverter;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class DatesToCron implements DatesToCronConverter {
    @Override
    public String convert(List<String> dates) throws DatesToCronConvertException {

        List<LocalDateTime> dateList = new ArrayList<>();

        //проверяем на валидность входные строки
        for(String date : dates){
            dateList.add(tryParseDate(date));
        }

        dateList = dateList.stream().distinct().collect(Collectors.toList());

        //сортируем даты по возрастанию
        Collections.sort(dateList);


        //ниже несколько действий для создания cron для входного списка дат
        //список суммирующий все кроны
        List<ArrayList> list = new ArrayList<>();
        //для секунд
        list.add(new ArrayList<Integer>());
        //для минут
        list.add(new ArrayList<Integer>());
        //для часов
        list.add(new ArrayList<Integer>());
        //для дней
        list.add(new ArrayList<Integer>());
        //для месяцев
        list.add(new ArrayList<Integer>());
        //для дней недели
        list.add(new ArrayList<String>());

        //хранения времен для конкретных дат
        TreeMap<LocalDate, List<LocalTime>> map = new TreeMap<>();

        for(LocalDateTime localDateTime : dateList){
            LocalDate ld = localDateTime.toLocalDate();
            LocalTime lt = localDateTime.toLocalTime();

            //для каждой даты создаем список со всеми временами
            if (!map.containsKey(ld)) {
                map.put(ld, new ArrayList<>());
            }
            map.get(ld).add(lt);
        }

        //определяем самые частые повторения времени
        //список с наборами времен для дат(без повторений)
        List<List<LocalTime>> listLt = map.values().stream().distinct().collect(Collectors.toList());
        //max - переменная хранящая позицию списка времен с максимальным количеством повторений для всех дат
        int max = 0;
        //count - количество повторения списка времен
        long count = map.values().stream().filter(i -> i.containsAll(listLt.get(0))).count() * listLt.get(0).size();

        for(int index = 0; index < listLt.size(); index++){
            int finalIndex = index;
            if(map.values().stream().filter(i -> i.containsAll(listLt.get(finalIndex))).count() * listLt.get(index).size() >
                    count){
                max = index;
                count = map.values().stream().filter(i -> i.containsAll(listLt.get(finalIndex))).count() * listLt.get(index).size();
            }
        }
        //проверяем что выбранные времена встречаются в большентсве дат
        if(count * 2 < dateList.size())
            throw new DatesToCronConvertException("<50");


        List<LocalDate> localDateList =  new ArrayList<>(map.keySet());
        List<LocalTime> localTimeList = listLt.get(max);
        Collections.sort(localDateList);

        //разбиваем даты на дни, месяца и дни недели
        for(LocalDate localDate : localDateList){
            if(!map.get(localDate).containsAll(localTimeList)) {
                map.remove(localDate);
            } else{
                if(!list.get(3).contains(localDate.getDayOfMonth()))
                    list.get(3).add(localDate.getDayOfMonth());

                if(!list.get(4).contains(localDate.getMonthValue()))
                    list.get(4).add(localDate.getMonthValue());

                if(!list.get(5).contains(localDate.getDayOfWeek()))
                    list.get(5).add(localDate.getDayOfWeek());
            }
        }

        //разбиваем времена на секунды, минуты и часы
        for(int i = 0; i < localTimeList.size(); i++){
            if(!list.get(0).contains(localTimeList.get(i).getSecond()))
                list.get(0).add(localTimeList.get(i).getSecond());
            if(!list.get(1).contains(localTimeList.get(i).getMinute()))
                list.get(1).add(localTimeList.get(i).getMinute());
            if(!list.get(2).contains(localTimeList.get(i).getHour()))
                list.get(2).add(localTimeList.get(i).getHour());
        }

        //определяем интервал между датами
        LocalTime lt1 = localTimeList.get(0);
        LocalTime lt2 = localTimeList.get(1% localTimeList.size());
        Duration duration = Duration.between(lt1, lt2);
        for(int i=0;i<localTimeList.size()-1;i++){
            lt1 = localTimeList.get(i);
            lt2 = localTimeList.get(i+1);

            //если интервал не повторился, то присваиваем ему 0
            if(duration.compareTo(Duration.between(lt1, lt2)) != 0){
                duration = Duration.ZERO;
                break;
            }
        }

        //если есть сдвиг по нескольким позициям(пример: на 1 час и 30 минут), то нельзя создать cron
        if((duration.toHours() !=0 && (duration.toMinutes()%60 !=0 || duration.getSeconds()%60 !=0)) ||
                (duration.toMinutes()%60 !=0 && duration.getSeconds()%60 != 0))
            throw new DatesToCronConvertException("did not possible to make cron");

        //такая же проверка для дат
        LocalDate ld1 = localDateList.get(0);
        LocalDate ld2 = localDateList.get(1%localDateList.size());
        Period period = Period.between(ld1, ld2);
        for(int i = 0; i < localDateList.size()-1;i++){
            ld1 = localDateList.get(i);
            ld2 = localDateList.get(i+1);

            if(LocalDate.MIN.plus(period).compareTo(LocalDate.MIN.plus(Period.between(ld1, ld2).normalized())) != 0){
                period = Period.ZERO;
                break;
            }
        }

        if(period.getMonths() != 0 && period.getDays() !=0)
            throw new DatesToCronConvertException("did not possible to make cron");


        String result = "";
        //mask - маска по который мы будем строить cron
        long[] mask = new long[]{duration.getSeconds()%60, duration.toMinutes()%60, duration.toHours(),
                period.getDays(), period.getMonths(), 0};

        //если несколько дней недели - *
        //если один день недели - записываем первые три буквы
        if(list.get(5).size() > 1) {
            list.get(5).clear();
            list.get(5).add("*");

            result = " *";
        }else {
            result = " " + list.get(5).get(0).toString().substring(0,3);
        }

        //проверка на первое смещение
        boolean checkStep = true;
        for(int index = 4; index >= 0; index--){
            //ели было смешение на 0 или1
            if(mask[index] == 0 || mask[index] == 1){
                if(checkStep){
                    result = " *" + result;
                }else if(mask[index] == 0 && list.get(index).size() != 1){
                    //если смещение 0 и количество элементов в списке под текущем индексом(это либо сек, мин, часы, дни, месяца) не равно 1
                    //значит указываем все элементы этого списка через запятую
                    result = " " + list.get(index).stream().map(e -> e.toString()).collect(Collectors.joining(",")) + result;
                }else if(list.get(index).indexOf(list.get(index).stream().max(Comparator.naturalOrder()).get()) + 1 ==
                            list.get(index).indexOf(list.get(index).stream().min(Comparator.naturalOrder()).get())){
                    result = " *" + result;
                    //если количество элементов в списке равняется 1, то записываем его
                }else if (list.get(index).size() == 1){
                    result = " " + list.get(index).get(0) + result;
                    //иначе выводим диапазон, так как их точно больше 1 и интервал между значениям равен 1
                }else {
                    result = " " + list.get(index).get(0) + "-" + list.get(index).get(list.get(index).size()-1) + result;
                }
            } else {
                //если смещение было больше 1
                result = " " + list.get(index).get(0) + "/" + mask[index] + result;
            }
            //если было смешение, то устанавливаем false
            if(mask[index] != 0){
                checkStep = false;
            }
        }
        return result;
    }

    @Override
    public String getImplementationInfo() {
        return "FIO - Krasnyukov Ivan Alekseevich" +
                "\nclass - " + this.getClass().getSimpleName() +
                "\npackage - " + this.getClass().getPackage().getName() +
                "\ngit - https://github.com/KrasnyukovIvan";
    }

    private static LocalDateTime tryParseDate(String date) throws DatesToCronConvertException {
        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch(DateTimeParseException e) {
            throw new DatesToCronConvertException("parse");
        }
    }
}
