package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService{

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO,User user) {
        Address address = modelMapper.map(addressDTO,Address.class);

        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);
        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAllAddresses() {
        return addressRepository.findAll()
                .stream().map(address -> modelMapper.map(address, AddressDTO.class)).toList();
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));

        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> userAddressList = user.getAddresses();
        return userAddressList.stream().map(address -> modelMapper.map(address, AddressDTO.class)).toList();
    }

    @Override
    public AddressDTO updateAddressById(Long addressId,AddressDTO addressDTO) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));

        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setPincode(addressDTO.getPincode());
        address.setStreet(addressDTO.getStreet());
        address.setBuildingName(addressDTO.getBuildingName());
        address.setCountry(addressDTO.getCountry());

        Address savedAddress = addressRepository.save(address);
        User user = address.getUser();

//        List<Address> userAddresses = user.getAddresses();
//        for(Address addressInDb: userAddresses){
//            if(addressInDb.getAddressId().equals(addressId)){
//                addressInDb.setCity(addressDTO.getCity());
//                addressInDb.setState(addressDTO.getState());
//                addressInDb.setPincode(addressDTO.getPincode());
//                addressInDb.setStreet(addressDTO.getStreet());
//                addressInDb.setBuildingName(addressDTO.getBuildingName());
//                addressInDb.setCountry(addressDTO.getCountry());
//                break;
//            }
//        }
//        user.setAddresses(userAddresses);

        user.getAddresses().removeIf(address1 -> address1.getAddressId().equals(addressId));
        user.getAddresses().add(savedAddress);
//        userRepository.save(user);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddressById(Long addressId) {

        Address address = addressRepository.findById(addressId)
                        .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));

        User user = address.getUser();

        user.getAddresses().removeIf(address1 -> address1.getAddressId().equals(addressId));
        userRepository.save(user);
        addressRepository.delete(address);

        return "Address deleted Successfully with addressId: "+addressId;
    }
}
