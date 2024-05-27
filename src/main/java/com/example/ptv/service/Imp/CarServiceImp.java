package com.example.ptv.service.Imp;

import com.example.ptv.dao.CarDao;
import com.example.ptv.dao.ordersDao;
import com.example.ptv.entity.Car;
import com.example.ptv.service.CarService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarServiceImp implements CarService {

    @Autowired
    private CarDao carDao;


    @Autowired
    ordersDao ordersdao;

    @Autowired
    private KafkaTemplate<String ,Object> kafkaTemplate;

    @Autowired
    private Gson gson = new GsonBuilder().create();
    @Override
    public List<Car> getAllCars() {
        return carDao.selectList(null); // 获取所有车辆信息
    }



    //获取队列中的orderId，小车处理过程用sleep代替
    //选择当前为空闲状态的小车，status置为1，代表忙碌中，current_task设置为orderId
    //处理完后，向消息队列发送orderId
    //货架位置？？？
    @Override
    public void processOrder(Integer orderId) {
        List<Car> cars = null;
        int attempts = 0;

        while (cars == null || cars.isEmpty()) {
            cars = carDao.selectCarsByStatus(0);
            if (cars != null && !cars.isEmpty()) {
                break; // 如果cars不为空，则退出循环
            }
            attempts++;
            if (attempts >= 5) { // 如果已尝试5次，则停止重试
                break;
            }
            try {
                Thread.sleep(3000); // 等待5秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
                break; // 在异常情况下退出循环
            }
        }

        if (cars != null && !cars.isEmpty()) {
            Car firstCar = cars.get(0); // 获取列表中的第一个Car对象
            carDao.updateCarStatusAndTask(firstCar.getId(), 1, "当前正在处理订单" + orderId);
            System.out.println("车车"+firstCar.getId()+"来咯！ "+"正在处理订单: " +orderId);
            try {
                Thread.sleep(3000); // 等待5秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
            }
            //处理完成
            carDao.updateCarStatusAndTask(firstCar.getId(), 0, "无");
            System.out.println("车车"+firstCar.getId()+"处理完毕！");
            kafkaTemplate.send("car-processing",gson.toJson(orderId));
        } else {

        }
    }


}
